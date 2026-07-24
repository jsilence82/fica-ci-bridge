# Architecture

## Pattern: Anti-Corruption Layer (ACL)

The FI-CA CI Bridge implements the **Anti-Corruption Layer** pattern from Domain-Driven Design.
The ACL sits between SAP — which has its own deeply proprietary domain model, field naming
conventions, and data encoding quirks — and modern REST consumers that should never need to
know what an `AUGST` or `VKONT` is.

Without the ACL, consumers would be forced to:
- Understand SAP OData V4 entity sets and SAP-specific query parameters
- Handle SAP-specific data quirks (leading zeros, zero dates, YYYYMMDD strings, sign conventions)
- Couple their release cycles to SAP API changes
- Fail entirely when SAP is unavailable

With the ACL, consumers get a stable, versioned REST/JSON API that abstracts all SAP details.
An optional PostgreSQL cache can be added to serve last-known data during SAP downtime.

---

## Data Flow

```
  SAP S/4HANA (FI-CA / Convergent Invoicing)
  ┌──────────────────────────────────────────┐
  │  Contract Account (FI-CA)                │
  │  FI-CA Document                          │
  │  CA Invoicing Document - Read            │
  │  Business Partner*                       │
  └────────────────────┬─────────────────────┘
  * not yet implemented — see docs/odata-api-reference.md
                       │  HTTP GET + OData V4
                       │  ($filter / $select / $expand)
                       │
                       ▼
  ┌───────────────────────────────────────────────────────────┐
  │                  FI-CA CI Bridge                          │
  │                                                           │
  │  ┌─────────────────────────────────────────────────────┐  │
  │  │  OData Client Layer  (client/)                      │  │
  │  │  CIDocumentClient                                   │  │
  │  │  ContractAccountClient                              │  │
  │  │  FicaDocumentClient                                 │  │
  │  │  • Shared $filter/$expand logic in ODataClientBase  │  │
  │  │  • Wraps 4xx/5xx into ODataClientException          │  │
  │  │  • Handles both V2 and V4 response envelopes        │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │  Raw ODataXxx objects        │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  Transformer Layer  (transformer/)                  │  │
  │  │  • Strips leading zeros from VKONT, GPART, OPBEL   │  │
  │  │  • Parses YYYYMMDD dates; maps 00000000 → null      │  │
  │  │  • Maps AUGST/ClearingStatus → domain status        │  │
  │  │  • Derives OVERDUE when OPEN + due date in past     │  │
  │  │  • Maps KSCHL condition type → charge type label    │  │
  │  │  • Trims all SAP whitespace-padded string fields    │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │  Clean domain DTOs only      │
  │      (no OData types cross this boundary)                 │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  Service Layer  (service/)                          │  │
  │  │  • Input-agnostic orchestration                     │  │
  │  │  • No imports from client/ or model/odata/          │  │
  │  │  • Optional: persist via JPA repository             │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │                             │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  REST Controllers  (controller/)                    │  │
  │  │  GET /api/invoices                                  │  │
  │  │  GET /api/contract-accounts/{vkont}                 │  │
  │  │  GET /api/payments                                  │  │
  │  └─────────────────────────────────────────────────────┘  │
  └───────────────────────────────┬───────────────────────────┘
                                  │  Clean REST / JSON API
                                  │  No SAP field names
                                  │
           ┌──────────────────────┼──────────────────────┐
           │                      │                      │
           ▼                      ▼                      ▼
  Customer billing        Collections              Third-party
  portals / mobile        dashboards               payment processors
```

---

## The Source-Adapter Boundary

**The service layer has no knowledge of where data came from.**

Nothing in `service/`, `controller/`, `model/dto/`, or `model/entity/` may import from
`client/` or `model/odata/`. This boundary is enforced by package structure and verified in
code review.

The practical consequence: the entire OData client stack can be replaced with a different
source adapter — an IDoc/ALE listener, an SAP Event Mesh consumer, a direct BAPI call —
without changing a single service, controller, or DTO class. The adapter just needs to
produce the same domain DTOs.

The same principle governs how the cache gets *updated*, not just how it's *read*: `sync/`
writes to `InvoiceRepository` exclusively through the `DocumentChangeIngester` interface (see
"Document Sync" below), so the polling scheduler that currently populates changes can later be
replaced by an event-driven consumer without `service/` or `controller/` changing either.

---

## Outbound API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/invoices` | List invoices; filterable by contract account; **paged** (`page`/`size`/`sort`) |
| GET | `/api/invoices/{invoiceNumber}` | Single invoice with line items |
| GET | `/api/contract-accounts/{vkont}` | Contract account master data |
| GET | `/api/contract-accounts/overdue` | Contract accounts with at least one OVERDUE invoice; **paged** (`page`/`size`/`sort`) |
| GET | `/api/payments` | Open FI-CA items (receivables); **paged** (`page`/`size`/`sort`) |

All three list endpoints return a `PagedModel<T>` (`{ content, page: { size, number,
totalElements, totalPages } }`); page size defaults to 50 and is capped at 200 via
`spring.data.web.pageable.*`. Paging is pushed to the database — the service calls
`Page`-returning repository methods and maps with `Page.map(...)`, never fetching a full list to
slice in memory. `/api/contract-accounts/overdue` filters via a correlated `exists` subquery
(`ContractAccountRepository.findWithInvoiceStatus`) since the account and invoice tables share no
JPA relationship.

---

## Key Design Decisions

### ODataClientBase
All OData calls go through `ODataClientBase`. It handles:
- `$filter`, `$select`, `$expand` query parameter construction
- Rate limiting of outbound calls via a Resilience4j `RateLimiter` (see README's
  "Outbound Rate Limiting" section)
- 4xx/5xx error wrapping into `ODataClientException`
- Response deserialization via `ODataWrapper<T>` (handles both V2 and V4 envelopes)

**Not yet implemented:** CSRF token fetch (`X-CSRF-Token: Fetch` preflight), which SAP OData
requires before any `POST`/`PATCH`. Deferred because the bridge is currently read-only — must
be added before any mutation endpoint is introduced.

### ODataWrapper
SAP OData V2 responses wrap results in `{ "d": { "results": [] } }`.
OData V4 responses use `{ "value": [] }`. The `ODataWrapper` class handles both.
Always check which version the specific API returns before deserializing.

### No SAP concepts in the API
Field names like `VKONT`, `AUGST`, `KSCHL`, and `GPART` never appear in REST responses.
The transformer layer is responsible for 100% of the translation.

### Flyway owns the schema
`spring.jpa.hibernate.ddl-auto=validate` in all environments. Hibernate validates but never
modifies the schema. All DDL changes go through versioned Flyway scripts.

### WireMock as SAP stub
In local and test environments, WireMock stubs the SAP OData endpoints so the project runs
without SAP infrastructure. Stubs live in `wiremock/mappings/` and `wiremock/__files/`.

### Document Sync (sync/)
`DocumentSyncScheduler` is the first writer the invoice cache has ever had. Once per
`sync.poll-interval`, it groups cached `OPEN`/`OVERDUE` invoices by contract account, calls
`FicaDocumentClient.findByContractAccount` once per account, and re-derives each document's
status with the existing `FiCaDocTransformer` — the same status-derivation logic
`FiCaDocTransformer` already uses elsewhere, not a second reimplementation of it.

Two design points that aren't obvious from the code alone:

- **Why it fetches unfiltered, not `findOpenItemsByContractAccount`.** SAP's `ClearingStatus eq
  'OPEN'` filter would hide the very documents that transitioned *out* of OPEN — the scheduler
  needs the current state of every document it's tracking, not just the ones still open, to
  detect the transition at all.
- **Why the match key is `ficaDocNumber`, not `invoiceNumber`.** `API_FICADOCUMENT` (what
  this scheduler polls) exposes FI-CA accounting documents (OPBEL — the ledger postings where
  clearing status lives), while `API_CAINVOICINGDOCUMENT` (what invoices are keyed by in the
  cache) exposes CI invoicing documents — a different SAP object with its own number. They are
  linked by the item-level `CADocumentNumber`, which `CIDocTransformer` lifts onto
  `InvoiceEntity.ficaDocNumber`. `DocumentSyncScheduler.diff()` resolves the FI-CA document number
  back to the cached invoice via that field before emitting a `DocumentChange` keyed by
  `invoiceNumber` — the natural key the rest of the system, including `DocumentChangeIngester`,
  actually uses.

**Explicitly out of scope for this job:** discovering invoices SAP has posted that the cache
has never seen (a cache miss is skipped and logged, not inserted and not thrown — see
`JpaDocumentChangeIngester`), and any event-driven consumer implementation. Both are described
as future work in the README's [Document Sync](../README.md#document-sync) section, which is
also the authoritative source for this job's known limitations (single-instance-only
scheduling, demo-scale polling).

---

## Technology Decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| SAP connectivity | Spring WebClient + OData V4 | Standard HTTP; no JCo/RFC dependencies; testable with WireMock |
| DTO mapping | MapStruct | Compile-time generated; faster than reflection-based mappers; errors at build time |
| Schema migrations | Flyway | Versioned, auditable DDL; prevents accidental schema drift |
| Auth (BTP) | SAP XSUAA (planned, not yet wired) | Native BTP identity provider; integrates with SAP's role concept — `SecurityConfig` currently permits all requests; see README's "Scope & Limitations" |
| Deployment | BTP Cloud Foundry | Co-located with SAP SaaS services; access to BTP Connectivity for on-premise tunnel |
| Local SAP stub | WireMock | Realistic HTTP stubbing; stubs double as integration test fixtures |
