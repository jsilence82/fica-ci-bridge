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
  │  API_CA_CONTRACTACCOUNT                  │
  │  API_FICADOCUMENT                        │
  │  API_BILLING_DOCUMENT_SRV                │
  │  API_BUSINESS_PARTNER                    │
  └────────────────────┬─────────────────────┘
                       │  HTTP GET + OData V4
                       │  ($filter / $select / $expand)
                       │
                       ▼
  ┌───────────────────────────────────────────────────────────┐
  │                  FI-CA CI Bridge                          │
  │                                                           │
  │  ┌─────────────────────────────────────────────────────┐  │
  │  │  OData Client Layer  (client/)                      │  │
  │  │  BillingDocumentClient                              │  │
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

---

## Outbound API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/invoices` | List billing documents; filterable by contract account |
| GET | `/api/invoices/{billingDocument}` | Single billing document with line items |
| GET | `/api/contract-accounts/{vkont}` | Contract account master data |
| GET | `/api/payments` | Open FI-CA items (receivables) |

---

## Key Design Decisions

### ODataClientBase
All OData calls go through `ODataClientBase`. It handles:
- `$filter`, `$select`, `$expand` query parameter construction
- CSRF token fetch (required for any POST/PATCH to SAP OData)
- 4xx/5xx error wrapping into `ODataClientException`
- Response deserialization via `ODataWrapper<T>` (handles both V2 and V4 envelopes)

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

---

## Technology Decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| SAP connectivity | Spring WebClient + OData V4 | Standard HTTP; no JCo/RFC dependencies; testable with WireMock |
| DTO mapping | MapStruct | Compile-time generated; faster than reflection-based mappers; errors at build time |
| Schema migrations | Flyway | Versioned, auditable DDL; prevents accidental schema drift |
| Auth (BTP) | SAP XSUAA | Native BTP identity provider; integrates with SAP's role concept |
| Deployment | BTP Cloud Foundry | Co-located with SAP SaaS services; access to BTP Connectivity for on-premise tunnel |
| Local SAP stub | WireMock | Realistic HTTP stubbing; stubs double as integration test fixtures |
