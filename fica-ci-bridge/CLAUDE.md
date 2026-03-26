# FI-CA / CI Integration Bridge — Project Context

## What This Project Is

A Spring Boot application acting as an **Anti-Corruption Layer** between SAP FI-CA (Contract
Accounting) / Convergent Invoicing and modern REST consumers. SAP pushes billing documents
as IDoc XML via ALE. The bridge receives, transforms, persists, and exposes them as clean
REST/JSON APIs that have no SAP-specific concepts leaking through.

This project intentionally differentiates from OData/CAP/RAP approaches by fully decoupling
consumers from the SAP data model, providing resilience against SAP downtime, and targeting
non-SAP consumers such as customer portals, mobile apps, and third-party systems.

---

## Architectural Pattern

```
SAP S/4HANA (FI-CA / Convergent Invoicing)
     │
     │  IDoc XML via ALE dispatch
     │  POST /api/idoc/billing
     ▼
FI-CA CI Bridge (this application)
     ├── Transforms IDoc → clean domain model
     ├── Persists to PostgreSQL
     └── Exposes REST/JSON API
               │
               ├── Customer billing portals
               ├── Mobile applications
               ├── Collections dashboards
               └── Third-party payment processors
```

In production this application is deployed on **SAP BTP Cloud Foundry** and connects to
on-premise SAP via the **BTP Connectivity Service + Cloud Connector** tunnel.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| XML Binding | JAXB |
| DTO Mapping | MapStruct |
| DB Migrations | Flyway |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Testing | JUnit 5 + Mockito + MockMvc |
| Containerization | Docker + Docker Compose |
| CI Pipeline | GitHub Actions |
| BTP Auth | SAP XSUAA (spring-xsuaa) |
| Build Tool | Maven |

---

## Package Structure

```
com/ficabridge/
├── config/
│   ├── SecurityConfig.java          # JWT / XSUAA auth config
│   ├── JacksonConfig.java           # JSON serialization settings
│   ├── OpenApiConfig.java           # Swagger / OpenAPI setup
│   └── PersistenceConfig.java       # JPA / datasource config
│
├── controller/
│   ├── inbound/
│   │   └── IDocInboundController.java      # Receives SAP IDoc XML posts
│   └── outbound/
│       ├── InvoiceController.java           # Invoice REST API
│       └── ContractAccountController.java   # Contract account REST API
│
├── service/
│   ├── InvoiceService.java
│   ├── ContractAccountService.java
│   └── IdocProcessingService.java           # Orchestrates inbound IDoc handling
│
├── transformer/                             # Core of the application — SAP domain logic
│   ├── BillingDocTransformer.java           # CI billing doc → InvoiceDTO
│   ├── FiCaDocTransformer.java              # FI-CA doc → ContractAccountDTO
│   └── TransformerUtils.java               # Shared utils (SAP date parsing, zero stripping)
│
├── model/
│   ├── idoc/                                # Raw SAP IDoc XML mappings (JAXB)
│   │   ├── BillingIDoc.java
│   │   ├── FiCaDocIDoc.java
│   │   └── IDocHeader.java                  # Shared EDI_DC40 header segment
│   ├── entity/                              # JPA database entities
│   │   ├── InvoiceEntity.java
│   │   ├── InvoiceLineItemEntity.java
│   │   └── ContractAccountEntity.java
│   └── dto/                                 # API request/response objects
│       ├── InvoiceDTO.java
│       ├── ContractAccountDTO.java
│       ├── PaymentDTO.java
│       └── AleAcknowledgementDTO.java       # SAP expects this back after IDoc receipt
│
├── repository/
│   ├── InvoiceRepository.java
│   └── ContractAccountRepository.java
│
├── exception/
│   ├── IDocProcessingException.java
│   ├── InvoiceNotFoundException.java
│   └── GlobalExceptionHandler.java          # @ControllerAdvice
│
├── mapper/                                  # MapStruct entity ↔ DTO mapping
│   ├── InvoiceMapper.java
│   └── ContractAccountMapper.java
│
└── FicaCiBridgeApplication.java
```

---

## SAP Domain Knowledge — FI-CA / CI Field Reference

This is the most important section. The transformer logic depends entirely on understanding
what these SAP fields mean in the business context.

### IDoc Segment Names

| Segment | Description |
|---|---|
| `EDI_DC40` | IDoc control record / header (present in all IDocs) |
| `E1INVDO` | Convergent Invoicing billing document header segment |
| `E1INVIO` | Convergent Invoicing billing document line item segment |

### Key Field Names and Business Meaning

| SAP Field | Segment | Business Meaning | Notes |
|---|---|---|---|
| `DOCNUM` | EDI_DC40 | IDoc document number | Used for idempotency check |
| `MESTYP` | EDI_DC40 | Message type (e.g. INVOIC) | |
| `CREDAT` | EDI_DC40 | Created date | SAP format: YYYYMMDD |
| `VBELN` | E1INVDO | Billing document number | CI document number |
| `GPART` | E1INVDO | Business partner number | ≠ contract account, do not confuse |
| `VKONT` | E1INVDO | Contract account number | FI-CA contract account |
| `VTREF` | E1INVDO | Contract reference | Links to the underlying contract |
| `FAEDN` | E1INVDO | Due date | SAP format: YYYYMMDD |
| `BETRW` | E1INVDO | Amount in document currency | |
| `WAERS` | E1INVDO | Currency key | e.g. USD, EUR |
| `AUGST` | E1INVDO | Clearing status | See clearing status table below |
| `AUGDT` | E1INVDO | Clearing date | 00000000 if not yet cleared |
| `OPBEL` | E1INVDO | FI-CA document number | Links CI billing doc to FI-CA open item |
| `POSNR` | E1INVIO | Line item number | |
| `KSCHL` | E1INVIO | Condition type | See condition type table below |
| `MWSKZ` | E1INVIO | Tax code | |
| `HWBAS` | E1INVIO | Tax base amount | |
| `TXJCD` | E1INVIO | Tax jurisdiction code | |

### Clearing Status Values (AUGST)

| AUGST Value | Meaning | Maps To |
|---|---|---|
| `` (empty) | Not cleared | `OPEN` |
| `0` | Not cleared | `OPEN` |
| `1` | Fully cleared | `CLEARED` |
| `2` | Partially cleared | `PARTIALLY_PAID` |
| `3` | Reversed | `REVERSED` |

**Important:** If status is OPEN and due date is in the past, derive status as `OVERDUE`
at transform time. This is business logic, not something SAP sends explicitly.

### Condition Types (KSCHL) — CI Charge Types

| KSCHL Value | Business Meaning |
|---|---|
| `ZCI1` | Usage Charge |
| `ZCI2` | Subscription Fee |
| `ZCI3` | One-Time Charge |
| `ZCI4` | Credit Adjustment |
| `ZTAX` | Tax |
| `ZDIS` | Discount |

*Note: Z* condition types are customer-specific. These are example values —
real implementations will vary by client. Always map unknown condition types
to a safe default label rather than throwing an exception.*

### SAP Data Quirks to Handle in the Transformer

- **Leading zeros:** SAP pads numeric IDs with leading zeros (e.g. `0000100456`).
  Always strip these when mapping to DTOs using `replaceAll("^0+", "")`.
- **Zero date:** SAP represents a null/empty date as `00000000`. Always check for this
  before parsing. Return `null` for zero dates.
- **Date format:** All SAP dates are `YYYYMMDD` strings, not ISO format.
  Use `DateTimeFormatter.ofPattern("yyyyMMdd")`.
- **Whitespace padding:** SAP string fields are often padded with trailing spaces. Always
  call `.trim()` or `.strip()` when mapping.
- **GPART vs VKONT:** Business partner (`GPART`) and contract account (`VKONT`) are
  different entities in FI-CA. A business partner can have multiple contract accounts.
  Never conflate these in the data model.

---

## Key Endpoints

### Inbound (SAP → Bridge)

| Method | Path | Description |
|---|---|---|
| POST | `/api/idoc/billing` | Receives CI billing IDoc XML from SAP ALE |

Consumes `application/xml`. Returns `AleAcknowledgementDTO` as XML.
SAP ALE expects an acknowledgement response — if it does not receive one it will retry.

### Outbound (Bridge → Consumers)

| Method | Path | Description |
|---|---|---|
| GET | `/api/invoices` | List invoices, filterable by status and contract account |
| GET | `/api/invoices/{billingDocNumber}` | Get single invoice by billing doc number |
| GET | `/api/contract-accounts/{contractAccount}` | Get contract account with all invoices |
| GET | `/api/contract-accounts/overdue` | Get all contract accounts with overdue items |

---

## Invoice Status Enum

```java
public enum InvoiceStatus {
    OPEN,            // Not yet paid, not past due
    CLEARED,         // Fully paid / cleared in FI-CA
    PARTIALLY_PAID,  // Partial clearing exists
    OVERDUE,         // Past due date, not cleared (derived, not from SAP)
    REVERSED         // Document reversed
}
```

---

## Idempotency

Always check if a document with the same `DOCNUM` (IDoc number) or `VBELN` (billing doc
number) already exists before processing. SAP ALE can send duplicate IDocs on retry.
Log a warning and return the existing document number without reprocessing.

---

## Database Migrations (Flyway)

Migrations live in `src/main/resources/db/migration/` and follow the naming convention:

```
V1__create_invoice_table.sql
V2__create_contract_account_table.sql
V3__create_line_items_table.sql
```

Never use `spring.jpa.hibernate.ddl-auto=create` or `update` in anything other than local
dev. Flyway owns the schema in all other environments.

---

## Application Profiles

| Profile | Purpose |
|---|---|
| `local` | Local development, Docker Compose PostgreSQL |
| `btp` | SAP BTP Cloud Foundry deployment, XSUAA auth, BTP datasource binding |

---

## SAP BTP Deployment

In production this application runs on **SAP BTP Cloud Foundry**.

- Deployment descriptor: `manifest.yml` in project root
- Auth: SAP XSUAA replaces local JWT config when `btp` profile is active
- Database: BTP PostgreSQL service binding (credentials injected via `VCAP_SERVICES`)
- SAP connectivity: BTP Connectivity Service + Cloud Connector tunnel for on-premise SAP
- Deploy command: `cf push fica-ci-bridge -p target/fica-ci-bridge.jar`

The `application-btp.yml` profile should read datasource config from `VCAP_SERVICES`
environment variable rather than hardcoded values.

---

## Sample IDoc Payload

Use this XML to test the inbound endpoint manually:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<IDOC>
  <EDI_DC40>
    <DOCNUM>0000000098765432</DOCNUM>
    <MESTYP>INVOIC</MESTYP>
    <CREDAT>20240315</CREDAT>
  </EDI_DC40>
  <E1INVDO>
    <VBELN>0090001234</VBELN>
    <GPART>0000100456</GPART>
    <VKONT>100000789</VKONT>
    <VTREF>CT-2024-00123</VTREF>
    <FAEDN>20240415</FAEDN>
    <BETRW>285.75</BETRW>
    <WAERS>USD</WAERS>
    <AUGST>0</AUGST>
    <AUGDT>00000000</AUGDT>
    <OPBEL>0000500987</OPBEL>
  </E1INVDO>
  <E1INVIO>
    <POSNR>0010</POSNR>
    <KSCHL>ZCI2</KSCHL>
    <BETRW>199.99</BETRW>
    <MWSKZ>A1</MWSKZ>
    <HWBAS>199.99</HWBAS>
    <TXJCD>US-CA-LA</TXJCD>
  </E1INVIO>
  <E1INVIO>
    <POSNR>0020</POSNR>
    <KSCHL>ZCI1</KSCHL>
    <BETRW>65.00</BETRW>
    <MWSKZ>A1</MWSKZ>
    <HWBAS>65.00</HWBAS>
    <TXJCD>US-CA-LA</TXJCD>
  </E1INVIO>
  <E1INVIO>
    <POSNR>0030</POSNR>
    <KSCHL>ZTAX</KSCHL>
    <BETRW>20.76</BETRW>
    <MWSKZ>A1</MWSKZ>
    <HWBAS>264.99</HWBAS>
    <TXJCD>US-CA-LA</TXJCD>
  </E1INVIO>
</IDOC>
```

Test with curl:

```bash
curl -X POST http://localhost:8080/api/idoc/billing \
  -H "Content-Type: application/xml" \
  -d @docker/sample-idocs/billing-invoice.xml
```

---

## Build Order

When implementing, always follow this sequence — each layer depends on the one before it:

```
model/idoc  →  model/dto  →  transformer  →  model/entity
     →  repository  →  service  →  controller
          →  exception handling  →  tests throughout
```

Build and test the transformer first. It is the most important and unique part of the
project and has no Spring dependencies — pure Java logic that is easy to unit test.

---

## What Makes This Project Unique

- Developer has 4 years of SAP FI-CA and Convergent Invoicing domain expertise
- Field mappings, clearing status logic, condition type handling, and SAP data quirks
  reflect real production knowledge — not guesswork
- Architecture mirrors real enterprise SAP integration patterns (ALE, BTP, Cloud Connector)
- Demonstrates both SAP domain depth and modern Java/Spring Boot capability
- The combination is rare: most Java developers know nothing about FI-CA; most ABAP
  developers have never built a Spring Boot REST API
