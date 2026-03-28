# FI-CA CI Bridge

An **Anti-Corruption Layer** between SAP S/4HANA FI-CA / Convergent Invoicing and REST consumers.
The bridge fetches data from SAP via standard **OData V4** HTTP calls and exposes a clean,
simplified JSON API for downstream systems such as reporting tools, customer portals,
and payment processors.

---

## Architecture

```
S/4HANA OData V4 APIs
  API_CA_CONTRACTACCOUNT
  API_FICADOCUMENT
  API_BILLING_DOCUMENT_SRV
  API_BUSINESS_PARTNER
          │
          │  HTTP (Basic Auth / OAuth2 via BTP XSUAA)
          ▼
  ┌──────────────────────────┐
  │   OData Client Layer     │  ContractAccountClient
  │   (client/)              │  FicaDocumentClient
  │                          │  BillingDocumentClient
  │   Base: ODataClientBase  │  (shared $filter/$expand logic)
  └──────────┬───────────────┘
             │  Clean domain DTOs only — source not visible below
             ▼
  ┌──────────────────────────┐
  │   Transformer Layer      │  SAP field names → English domain fields
  │   (transformer/)         │  SAP dates / amounts / zero-padding
  └──────────┬───────────────┘
             ▼
  ┌──────────────────────────┐
  │   Service Layer          │  Input-agnostic orchestration
  │   (service/)             │  (no OData imports allowed)
  └──────────┬───────────────┘
             ▼
  ┌──────────────────────────┐
  │   REST Controllers       │  JSON API for downstream consumers
  │   (controller/)          │
  └──────────────────────────┘
             │
             ▼
  H2 (dev) / PostgreSQL (prod)  — optional local cache
```

In local and test environments a **WireMock** server stubs the SAP OData responses.
No SAP system is required to build or run the project.

---

## SAP APIs Consumed

| API Name                                  | API ID                        | Used For                            |
|-------------------------------------------|-------------------------------|-------------------------------------|
| Contract Account (FI-CA)                  | `API_CA_CONTRACTACCOUNT`      | Contract account master data        |
| FI-CA Document                            | `API_FICADOCUMENT`            | Posted FI-CA accounting documents   |
| Convergent Invoicing – Billing Document   | `API_BILLING_DOCUMENT_SRV`    | Billing document header + items     |
| Business Partner                          | `API_BUSINESS_PARTNER`        | BP data linked to contract accounts |

All APIs are available on the [SAP Business Accelerator Hub](https://api.sap.com).
Use V4 where available; fall back to V2 only if V4 is not published for the target API.

---

## REST API

| Method | Path                                  | Description                        |
|--------|---------------------------------------|------------------------------------|
| GET    | `/api/invoices`                       | List billing documents             |
| GET    | `/api/invoices/{billingDocument}`     | Single billing document with items |
| GET    | `/api/contract-accounts/{vkont}`      | Contract account master data       |
| GET    | `/api/payments`                       | Open FI-CA items (receivables)     |

Swagger UI is available at `/swagger-ui.html` when the application is running.

---

## Running Locally (no SAP required)

```bash
# Start WireMock + PostgreSQL + the application
docker-compose up

# Application is available at http://localhost:8080
# WireMock admin UI at http://localhost:8089/__admin
```

WireMock stub files live in `wiremock/mappings/` and `wiremock/__files/`.
See [docs/wiremock-setup.md](docs/wiremock-setup.md) for details.

---

## Build & Test

```bash
# Run all tests (WireMock stubs used in integration tests)
mvn verify

# Build the JAR
mvn clean package -DskipTests

# Run a single test class
mvn test -Dtest=TransformerUtilsTest
```

Java 21 is required. Set `JAVA_HOME` before running Maven if your default JVM is older:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
```

---

## Configuration

| Environment Variable      | Description                              | Dev Default              |
|---------------------------|------------------------------------------|--------------------------|
| `SAP_ODATA_BASE_URL`      | Base URL of S/4HANA OData services       | `http://localhost:8089`  |
| `SAP_ODATA_AUTH_TYPE`     | `basic` or `oauth2`                      | `basic`                  |
| `SAP_ODATA_USERNAME`      | Basic auth username                      | `wiremock`               |
| `SAP_ODATA_PASSWORD`      | Basic auth password                      | `wiremock`               |
| `SPRING_DATASOURCE_URL`   | JDBC URL                                 | H2 in-memory             |

In production (BTP), `SAP_ODATA_BASE_URL` points at the real S/4HANA system via a BTP Destination.
OAuth2 credentials come from the BTP XSUAA service binding.

---

## Deployment to SAP BTP Cloud Foundry

```bash
# Build
mvn clean package -DskipTests

# Push to BTP CF
cf push
```

The `manifest.yml` at the repo root configures the CF application. Required CF services:
`postgresql-ficabridge`, `xsuaa-ficabridge`, `connectivity-ficabridge`.

See [docs/btp-deployment.md](docs/btp-deployment.md) for full deployment instructions.

---

## Project Structure

```
src/main/java/com/ficabridge/
├── client/          OData HTTP clients (one per SAP API)
├── config/          Spring beans (WebClient, Security, Jackson, OpenAPI)
├── controller/      REST controllers (inbound JSON API)
├── exception/       Exception types + GlobalExceptionHandler
├── mapper/          MapStruct entity ↔ DTO mappers
├── model/
│   ├── dto/         Outbound REST response objects
│   ├── entity/      JPA entities for local cache
│   └── odata/       Raw OData response objects (deserialized JSON)
├── service/         Input-agnostic orchestration services
└── transformer/     SAP field names / dates / amounts → domain types

wiremock/
├── mappings/        WireMock stub request matchers
└── __files/         OData JSON response bodies
```
