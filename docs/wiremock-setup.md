# WireMock Setup

The FI-CA CI Bridge uses [WireMock](https://wiremock.org) to stub SAP S/4HANA OData
responses in local development and integration tests. No SAP system is required to
build, run, or test the project.

---

## Directory Structure

```
wiremock/
├── mappings/                     # Request matchers — tell WireMock how to route requests
│   ├── billing-document-list.json
│   ├── billing-document-single.json
│   ├── contract-account-single.json
│   └── fica-document-list.json
└── __files/                      # Response bodies — referenced by bodyFileName in mappings
    ├── billing-document-response.json
    ├── contract-account-response.json
    └── fica-document-response.json
```

---

## Running WireMock Locally

WireMock runs as a Docker sidecar on port **8089** (mapped from container port 8080).

```bash
# Start everything (WireMock + PostgreSQL + the application)
docker-compose up

# Start only WireMock (useful during development)
docker-compose up wiremock
```

Once running:
- WireMock admin UI: `http://localhost:8089/__admin`
- List active mappings: `http://localhost:8089/__admin/mappings`

The application's `SAP_ODATA_BASE_URL` environment variable defaults to `http://localhost:8089`,
so requests are automatically routed to WireMock when running locally.

---

## How Stubs Work

Each file in `wiremock/mappings/` defines one or more stub mappings. A mapping has a
`request` matcher and a `response`:

```json
{
  "mappings": [
    {
      "name": "billing-document-list-by-contract-account",
      "request": {
        "method": "GET",
        "urlPathPattern": "/API_BILLING_DOCUMENT_SRV/BillingDocument",
        "queryParameters": {
          "$filter": { "contains": "ContractAccount eq" }
        }
      },
      "response": {
        "status": 200,
        "headers": { "Content-Type": "application/json" },
        "bodyFileName": "billing-document-response.json"
      }
    }
  ]
}
```

Response body files in `__files/` contain realistic OData V4 JSON payloads with
actual FI-CA field names and SAP data quirks (YYYYMMDD dates, zero-padded IDs, etc).

---

## Using WireMock in Integration Tests

The `wiremock-spring-boot` dependency (test scope) allows integration tests to start
a WireMock server automatically alongside the Spring context.

```java
@SpringBootTest
@EnableWireMock
class InvoiceIntegrationTest {

    @InjectWireMock
    WireMockServer wireMock;

    @Test
    void fetchesInvoicesFromSapODataStub() {
        wireMock.stubFor(get(urlPathMatching("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("billing-document-response.json")));

        // ... call the REST endpoint and assert the response
    }
}
```

The `__files/` directory on the classpath (copied from `wiremock/__files/`) is used
automatically by WireMock's `withBodyFile()` method.

---

## Adding New Stubs

To stub a new SAP OData endpoint:

1. Add a new mapping file to `wiremock/mappings/` (or add a new entry to an existing file)
2. Add the response body to `wiremock/__files/`
3. Ensure the response body uses realistic SAP data:
   - Dates as `"YYYYMMDD"` strings
   - Zero-padded IDs (`"0000200001"`)
   - Amounts as decimal strings (`"1234.56"`)
   - Zero-date for uncleared items (`"00000000"`)

---

## Stub Coverage

| SAP API Path | Stub File | Coverage |
|---|---|---|
| `GET /API_BILLING_DOCUMENT_SRV/BillingDocument?$filter=ContractAccount...` | billing-document-list.json | List by contract account |
| `GET /API_BILLING_DOCUMENT_SRV/BillingDocument('{id}')` | billing-document-single.json | Single by document number |
| `GET /API_CA_CONTRACTACCOUNT/ContractAccount('{id}')` | contract-account-single.json | Single by VKONT |
| `GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount...` | fica-document-list.json | List by contract account |

---

## Resetting WireMock Between Tests

If tests share the same WireMock server, reset stubs between tests to prevent interference:

```java
@AfterEach
void resetWireMock() {
    wireMock.resetAll();
}
```

Or use `@WireMockTest` (from wiremock-spring-boot) which resets automatically between tests.
