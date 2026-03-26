# Architecture

## Pattern: Anti-Corruption Layer (ACL)

The FI-CA CI Bridge implements the **Anti-Corruption Layer** pattern from Domain-Driven Design.
The ACL sits between SAP — which has its own deeply proprietary domain model, field naming
conventions, and data encoding quirks — and modern REST consumers that should never need to
know what an `AUGST` or `VKONT` is.

Without the ACL, consumers would be forced to:
- Understand SAP IDoc XML structure and segment names
- Handle SAP-specific data quirks (leading zeros, zero dates, YYYYMMDD strings)
- Couple their release cycles to SAP change transports
- Fail entirely when SAP is unavailable

With the ACL, consumers get a stable, versioned REST/JSON API backed by a local PostgreSQL store
that survives SAP downtime.

---

## Data Flow

```
  SAP S/4HANA (FI-CA / Convergent Invoicing)
  ┌──────────────────────────────────────────┐
  │  Billing document posted or cleared      │
  │  → ALE framework dispatches IDoc         │
  │  → HTTP POST to bridge inbound endpoint  │
  └────────────────────┬─────────────────────┘
                       │  POST /api/idoc/billing
                       │  Content-Type: application/xml
                       │  Body: INVOIC IDoc XML (EDI_DC40 + E1INVDO + E1INVIO)
                       │
                       ▼
  ┌───────────────────────────────────────────────────────────┐
  │                  FI-CA CI Bridge                          │
  │                                                           │
  │  ┌─────────────────────────────────────────────────────┐  │
  │  │  IDocInboundController  POST /api/idoc/billing      │  │
  │  │  • Deserialises IDoc XML via JAXB                   │  │
  │  │  • Delegates to IdocProcessingService               │  │
  │  │  • Returns AleAcknowledgementDTO as XML             │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │                             │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  IdocProcessingService                              │  │
  │  │  • Idempotency check on EDI_DC40.DOCNUM             │  │
  │  │  • Duplicate IDocs are acknowledged without         │  │
  │  │    reprocessing (SAP ALE retries on missing ack)    │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │                             │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  BillingDocTransformer                              │  │
  │  │  • Strips leading zeros from VBELN, GPART, VKONT   │  │
  │  │  • Parses YYYYMMDD dates; maps 00000000 → null      │  │
  │  │  • Maps AUGST → InvoiceStatus enum                  │  │
  │  │  • Derives OVERDUE when OPEN + due date in past     │  │
  │  │  • Maps KSCHL condition type → charge type label    │  │
  │  │  • Trims all SAP whitespace-padded string fields    │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │                             │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  InvoiceService  →  InvoiceRepository               │  │
  │  │  Persists InvoiceEntity + InvoiceLineItemEntity      │  │
  │  └──────────────────────────┬──────────────────────────┘  │
  │                             │                             │
  │  ┌──────────────────────────▼──────────────────────────┐  │
  │  │  PostgreSQL 16                                       │  │
  │  │  Tables: invoices, invoice_line_items,              │  │
  │  │          contract_accounts                          │  │
  │  │  Schema owned by Flyway migrations                  │  │
  │  └─────────────────────────────────────────────────────┘  │
  └───────────────────────────────┬───────────────────────────┘
                                  │
                                  │  Clean REST / JSON API
                                  │  No SAP field names, no IDoc concepts
                                  │
           ┌──────────────────────┼──────────────────────┐
           │                      │                      │
           ▼                      ▼                      ▼
  Customer billing        Collections              Third-party
  portals / mobile        dashboards               payment processors
```

---

## Inbound Endpoint

| Method | Path | Consumes | Produces |
|--------|------|----------|----------|
| POST | `/api/idoc/billing` | `application/xml` | `application/xml` |

SAP ALE posts the IDoc and expects an `ALEAUD01` XML acknowledgement in response. If it
does not receive one it will retry according to the partner profile retry schedule.

---

## Outbound API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/invoices` | List invoices; filterable by `status` and `contractAccount` |
| GET | `/api/invoices/{billingDocNumber}` | Single invoice by billing document number |
| GET | `/api/contract-accounts/{contractAccount}` | Contract account with all related invoices |
| GET | `/api/contract-accounts/overdue` | All contract accounts carrying overdue items |

---

## Key Design Decisions

### Idempotency
Every inbound IDoc carries a unique `DOCNUM` in the `EDI_DC40` control record. The bridge
checks this before processing. If the document already exists it logs a warning and returns
a success acknowledgement — SAP gets its ack, no duplicate data is created.

### Resilience to SAP downtime
Because all data is persisted locally, consumer-facing APIs continue to serve the last-known
state even when SAP is unavailable. This is a core advantage over direct OData/RFC calls from
consumers.

### No SAP concepts in the API
Field names like `VKONT`, `AUGST`, `KSCHL`, and `GPART` never appear in REST responses.
The transformer layer is responsible for 100% of the translation. Consumers are entirely
decoupled from the SAP data model.

### Flyway owns the schema
`spring.jpa.hibernate.ddl-auto=validate` in all environments. Hibernate validates but never
modifies the schema. All DDL changes go through versioned Flyway scripts.

---

## Technology Decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| XML parsing | JAXB | Standard Java XML binding; JAXB annotations map directly to IDoc segment structure |
| DTO mapping | MapStruct | Compile-time generated; faster than reflection-based mappers; errors at build time |
| Schema migrations | Flyway | Versioned, auditable DDL; prevents accidental schema drift |
| Auth (BTP) | SAP XSUAA | Native BTP identity provider; integrates with SAP's role concept |
| Deployment | BTP Cloud Foundry | Co-located with SAP SaaS services; access to BTP Connectivity for on-premise tunnel |
