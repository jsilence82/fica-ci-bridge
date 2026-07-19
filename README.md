# FI-CA CI Bridge

> **Proof of Concept.** This project demonstrates the architectural pattern for bridging SAP
> S/4HANA FI-CA / Convergent Invoicing to REST consumers. Core invoicing and contract account
> flows are implemented. Many FI-CA functional areas (dunning, interest, correspondence,
> payments, installment plans) are deliberately out of scope. See [Scope & Limitations](#scope--limitations).

An **Anti-Corruption Layer** between SAP S/4HANA FI-CA / Convergent Invoicing and REST consumers.
The bridge fetches data from SAP via standard **OData V4** HTTP calls and exposes a clean,
simplified JSON API for downstream systems such as reporting tools, customer portals,
and payment processors.

---

## Architecture

```
S/4HANA OData APIs
  Contract Account (FI-CA)
  FI-CA Document
  CA Invoicing Document - Read (V4)
  Business Partner*
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

\* Business Partner is not yet implemented — see [Scope & Limitations](#scope--limitations).

In local and test environments a **WireMock** server stubs the SAP OData responses.
No SAP system is required to build or run the project.

---

## SAP APIs Consumed

| API Name (search this on the Hub)                       | API ID                        | Protocol | Used For                            | Status          |
|---------------------------------------------------------|-------------------------------|----------|--------------------------------------|-----------------|
| Contract Account (FI-CA)                                | API_CA_CONTRACTACCOUNT        | V2       | Contract account master data         | Implemented     |
| FI-CA Document                                          | API_FICADOCUMENT              | V2       | Posted FI-CA accounting documents    | Implemented     |
| CA Invoicing Document - Read                            | API_CAINVOICINGDOCUMENT       | V4       | FI-CA invoicing doc header + items   | Implemented     |
| Business Partner                                        | API_BUSINESS_PARTNER          | V2       | BP data linked to contract accounts  | Not implemented |
| Contract Accounting Business Partner Invoice - Read     | API_CABUSPARTINVOICE          | V4       | Explicit status, clearing date, PDFs | Not implemented |

Full field-level detail for each API is in
[docs/odata-api-reference.md](docs/odata-api-reference.md).

> **Finding these APIs:** Search by the API name on the
> [SAP Business Accelerator Hub](https://api.sap.com). Use V4 where available; fall back
> to V2 only if V4 is not published for the target release.

---

## REST API

| Method | Path                                  | Description                                       |
|--------|---------------------------------------|----------------------------------------------------|
| GET    | `/api/invoices`                       | List billing documents                             |
| GET    | `/api/invoices/{billingDocument}`     | Single billing document with items                 |
| GET    | `/api/contract-accounts/{vkont}`      | Contract account master data                       |
| GET    | `/api/contract-accounts/overdue`      | Contract accounts with at least one OVERDUE invoice |
| GET    | `/api/payments`                       | Open FI-CA items (receivables)                     |

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
| `SAP_ODATA_USERNAME`      | Basic auth username                      | `wiremock`               |
| `SAP_ODATA_PASSWORD`      | Basic auth password                      | `wiremock`               |
| `SPRING_DATASOURCE_URL`   | JDBC URL                                 | H2 in-memory             |

`WebClientConfig` always authenticates outbound SAP calls with Basic auth — there is no OAuth2
client-credentials path yet for outbound OData calls. In production (BTP), `SAP_ODATA_BASE_URL`
would point at the real S/4HANA system via a BTP Destination; OAuth2-via-XSUAA for outbound SAP
calls is aspirational (see [Scope & Limitations](#scope--limitations)) — do not confuse it with
inbound XSUAA JWT validation on this bridge's own REST API, which is also not yet wired.

---

## Outbound Rate Limiting

All calls from `ODataClientBase` to the SAP OData backend go through a
[Resilience4j](https://resilience4j.readme.io/) `RateLimiter` named `sapOData`. This protects the
SAP system from being overwhelmed by concurrent traffic from this bridge — it is **not** related
to rate-limiting inbound requests to this service's own REST API.

The limiter wraps the reactive `WebClient` call in `ODataClientBase.fetchList` / `fetchSingle`
(via `resilience4j-reactor`'s `RateLimiterOperator`) before the response is blocked on. When a
call can't acquire a permit within `timeout-duration`, Resilience4j throws
`RequestNotPermitted`, which `GlobalExceptionHandler` maps to `HTTP 429 Too Many Requests` with
the standard structured error body, rather than letting it surface as an unhandled exception.

Configuration lives in `application.yml`:

```yaml
resilience4j:
  ratelimiter:
    instances:
      sapOData:
        limit-for-period: 10       # permits issued per refresh period
        limit-refresh-period: 1s   # how often the permit bucket refills
        timeout-duration: 500ms    # how long a caller waits for a permit before failing
```

**Tuning:** `limit-for-period` / `limit-refresh-period` are starting values, not measured
figures — increase them once you know the real throughput the target SAP system can sustain
(check with the SAP Basis/FI-CA team or load-test against a sandbox). `timeout-duration`
controls the tradeoff between latency and throughput: a short timeout fails fast under load
(cheap for the caller, cheap for SAP) while a longer timeout smooths out bursts at the cost of
slower responses when the limiter is saturated.

---

## Running on Kubernetes (local)

`fica-ci-bridge` deploys to Kubernetes as three workloads in a `fica-ci-bridge` namespace:
the app itself, an in-cluster Postgres (replacing the H2/PostgreSQL local cache), and an
in-cluster WireMock standing in for the real SAP OData backend (there is no real SAP system
in this PoC — see [Scope & Limitations](#scope--limitations)).

```
host :80 ──▶ Ingress (nginx)
             host: fica-ci-bridge.local
                      │
                      ▼
             Service: fica-ci-bridge  ◀── HorizontalPodAutoscaler
                      │                   (2–5 pods, target 70% CPU)
                      ▼
   Deployment: fica-ci-bridge (2 replicas)
     initContainer: wait-for-postgres
     startup / liveness / readiness probes → /actuator/health/*
     env ← ConfigMap (fica-ci-bridge-config) + Secret (fica-ci-bridge-secrets)
                      │
           ┌──────────┴──────────┐
           ▼                     ▼
   Service: postgres      Service: wiremock
   → Deployment + PVC     → Deployment
     (local JPA cache)      (stubs baked into image;
                              stands in for SAP OData)
```

### Prerequisites

- Docker Desktop (or another Docker daemon)
- [`kind`](https://kind.sigs.k8s.io/) — `brew install kind`
- `kubectl`

### Setup

```bash
# 1. Build the app image and the self-contained WireMock image
docker build -t fica-ci-bridge:local .
docker build -t fica-ci-bridge-wiremock:local ./wiremock

# 2. Create a kind cluster with ports 80/443 exposed on the host, for Ingress
kind create cluster --name fica-ci-bridge --config k8s/kind-config.yaml

# 3. Install ingress-nginx (kind's documented recipe) and metrics-server (required for the HPA)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl wait --namespace ingress-nginx --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=180s

kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
# kind's kubelet certs aren't signed for metrics-server's default verification:
kubectl patch deployment metrics-server -n kube-system --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'

# 4. Load the locally-built images into the kind cluster (no registry needed)
kind load docker-image fica-ci-bridge:local --name fica-ci-bridge
kind load docker-image fica-ci-bridge-wiremock:local --name fica-ci-bridge

# 5. Apply the manifests
kubectl apply -f k8s/namespace.yaml -f k8s/configmap.yaml -f k8s/secret.yaml \
  -f k8s/postgres.yaml -f k8s/wiremock.yaml \
  -f k8s/deployment.yaml -f k8s/service.yaml -f k8s/ingress.yaml -f k8s/hpa.yaml

# 6. Wait for the rollout, then hit it through the Ingress
kubectl -n fica-ci-bridge rollout status deployment/fica-ci-bridge
curl -H "Host: fica-ci-bridge.local" http://localhost/actuator/health/readiness
curl -H "Host: fica-ci-bridge.local" http://localhost/api/invoices
```

Check the HPA is actually receiving metrics (not just configured):

```bash
kubectl -n fica-ci-bridge get hpa
# NAME             REFERENCE                   TARGETS       MINPODS   MAXPODS   REPLICAS
# fica-ci-bridge   Deployment/fica-ci-bridge   cpu: 3%/70%   2         5         2
```

Tear down when done: `kind delete cluster --name fica-ci-bridge`

### Notes on the manifests

- **Probes** (`k8s/deployment.yaml`) use a `startupProbe` against `/actuator/health` to give the
  JVM + Flyway migrations room to boot, then separate `livenessProbe` /
  `readinessProbe` against Spring Boot Actuator's `/actuator/health/liveness` and
  `/actuator/health/readiness` health-indicator groups.
- **`initContainer: wait-for-postgres`** blocks the app container from starting until
  `pg_isready` succeeds against the `postgres` Service. Without it, the app and Postgres pods
  start concurrently, the app's first connection attempt fails before Postgres is accepting
  connections, and the container restarts once via CrashLoopBackOff before settling — the
  initContainer turns that into a single clean startup instead.
- **ConfigMap vs Secret**: `k8s/configmap.yaml` holds non-secret values (active profile, service
  URLs); `k8s/secret.yaml` holds credentials. Both are consumed via `envFrom`, the same
  environment-variable surface `docker-compose.yml` already uses (`SPRING_DATASOURCE_URL`,
  `SAP_ODATA_BASE_URL`, etc.) — no code changes were needed to make the app config-map-friendly.
  The credentials in `k8s/secret.yaml` are the same throwaway local-dev values `docker-compose.yml`
  already uses, not real secrets; a production deployment would source this from a secret manager
  rather than committing even low-value plaintext credentials.
- **HPA** (`k8s/hpa.yaml`) scales `fica-ci-bridge` between 2 and 5 replicas on 70% average CPU
  utilization. Requires `metrics-server` in the cluster (installed above).

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

## Scope & Limitations

This is a proof-of-concept project. It is **not production-ready**. The following areas are
intentionally out of scope and would need to be addressed before any real deployment.

### FI-CA domains not implemented

| Domain | What's missing |
|---|---|
| **Correspondence / printed invoices** | No integration with `API_CABUSPARTINVOICE` correspondence entities; no PDF retrieval (`CACorrespondenceBinary`) |
| **Dunning** | No dunning notices, dunning history, or dunning block management |
| **Interest calculation** | No interest documents or interest-run results surfaced via REST |
| **Payment lot / payment matching** | Incoming payments are not modelled; open items are inferred from invoice status only |
| **Installment plans** | Deferred payment schedules are not implemented |
| **Dispute management** | No billing dispute cases or write-off requests |
| **Write-off / adjustments** | Manual adjustments and uncollectable-debt write-offs are not modelled |
| **Security deposits** | Contract account deposit posting, release, and interest not implemented |
| **Reversals (write-back)** | The bridge is read-only; no `POST` / `PATCH` back to SAP OData |

### Technical gaps (pre-production requirements)

| Gap | Impact |
|---|---|
| **No data sync path** | REST endpoints always return empty results unless data is seeded manually; no scheduled or event-driven ingest from SAP |
| **No authentication** | All endpoints are `permitAll()`; Basic Auth (local) and XSUAA JWT (BTP) are not wired |
| **No pagination** | List endpoints return unbounded result sets |
| **`clearingDate` always null** | `InvoiceDTO.clearingDate` is never populated — requires `API_CABUSPARTINVOICE` item-level clearing data |
| **`idocDocnum` artifact** | `InvoiceEntity` carries an IDoc deduplication field that has no natural value in an OData-sourced system |
| **CSRF token not fetched** | Required before any mutation is introduced; deferred because the bridge is currently read-only |

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
├── Dockerfile       Self-contained WireMock image (stubs baked in, for k8s)
├── mappings/        WireMock stub request matchers
└── __files/         OData JSON response bodies

k8s/
├── kind-config.yaml Local kind cluster config (ingress port mappings)
├── namespace.yaml
├── configmap.yaml   Non-secret runtime config
├── secret.yaml      Local-dev credentials (Postgres, WireMock)
├── postgres.yaml    In-cluster Postgres (Deployment + PVC + Service)
├── wiremock.yaml    In-cluster WireMock (Deployment + Service)
├── deployment.yaml  fica-ci-bridge Deployment (probes, initContainer, resources)
├── service.yaml
├── ingress.yaml
└── hpa.yaml
```
