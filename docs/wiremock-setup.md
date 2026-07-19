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
      "name": "ca-invoicing-document-list-by-contract-account",
      "request": {
        "method": "GET",
        "urlPathPattern": "/sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001/CAInvcgDocument",
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

> The path is the full OData V4 service root for `API_CAINVOICINGDOCUMENT` — see
> [docs/odata-api-reference.md](odata-api-reference.md). Do not stub `API_BILLING_DOCUMENT_SRV`;
> that's the unrelated SD billing API and was never correct for this bridge.

Response body files in `__files/` contain realistic OData V4 JSON payloads with
actual FI-CA field names and SAP data quirks (YYYYMMDD dates, zero-padded IDs, etc).

---

## Using WireMock in Tests

WireMock is used at the **OData client layer**, not at the full-Spring-context integration
layer — the two test suites in this project have different jobs and different fixtures:

- **Client tests** (`BillingDocumentClientTest`, `ContractAccountClientTest`,
  `FicaDocumentClientTest`) exercise `client/` classes directly against a real WireMock server,
  using the JUnit 5 `@WireMockTest` extension (from `wiremock-junit5`, pulled in transitively by
  the `wiremock-spring-boot` dependency) with `WireMockRuntimeInfo` injected per test:

  ```java
  @WireMockTest
  class BillingDocumentClientTest {

      @BeforeEach
      void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
          WebClient webClient = WebClient.builder()
                  .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                  .build();
          client = new BillingDocumentClient(webClient, OBJECT_MAPPER, RateLimiter.ofDefaults("test"));
      }

      @Test
      void findByContractAccount_v4Response_returnsList() {
          stubFor(get(urlPathEqualTo(BASE_PATH))
                  .withQueryParam("$filter", equalTo("ContractAccount eq '100200'"))
                  .willReturn(okJson(json)));

          List<ODataBillingDocument> result = client.findByContractAccount("100200");
          // ... assertions
      }
  }
  ```

  These tests stub responses inline (`stubFor(...)`) rather than loading the `wiremock/mappings/`
  files from disk — the `wiremock/` directory's stubs are for **manual/local** use (via
  `docker-compose up`), not consumed by this test suite directly.

  `DocumentSyncSchedulerWireMockTest` (`sync/`) follows the same pattern one layer up: a real
  `FicaDocumentClient` against WireMock, but with `InvoiceRepository`, `SyncRunRepository`, and
  `DocumentChangeIngester` mocked with Mockito rather than backed by JPA — the client-HTTP
  boundary is what's worth exercising over real WireMock; the repository/ingester collaboration
  is plain object interaction and doesn't need it.

- **Full-context integration tests** (`InvoiceIntegrationTest`, `ContractAccountIntegrationTest`)
  boot the whole Spring context with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and a
  `TestRestTemplate`, but **do not use WireMock at all** — they seed the H2 database directly via
  the JPA repositories (`InvoiceRepository.save(...)`) and assert against the REST layer. Since
  the service layer never touches the OData client stack (see the source-adapter boundary in
  [architecture.md](architecture.md)), there's nothing SAP-shaped left to stub at that layer.

---

## Adding New Stubs

To stub a new SAP OData endpoint:

1. Add a new mapping file to `wiremock/mappings/` (or add a new entry to an existing file)
2. Add the response body to `wiremock/__files/`
3. Ensure the response body uses realistic SAP data:
   - Zero-padded IDs (`"0000200001"`)
   - Amounts as decimal strings (`"1234.56"`)
   - Dates match the target API's version: `"YYYYMMDD"` strings (`"00000000"` for uncleared
     items) on OData **V2** APIs (`API_CA_CONTRACTACCOUNT`, `API_FICADOCUMENT`); ISO
     `"yyyy-MM-dd"` strings on OData **V4** APIs (`API_CAINVOICINGDOCUMENT`) — see
     [docs/odata-api-reference.md](odata-api-reference.md#date-formats)

---

## Stub Coverage

| SAP API Path | Stub File | Coverage |
|---|---|---|
| `GET .../cainvoicingdocument/0001/CAInvcgDocument?$filter=ContractAccount...` | billing-document-list.json | List by contract account |
| `GET .../cainvoicingdocument/0001/CAInvcgDocument(CAInvoicingDocument='{id}')` | billing-document-single.json | Single by document number, with items expanded |
| `GET /API_CA_CONTRACTACCOUNT/ContractAccount('{id}')` | contract-account-single.json | Single by VKONT |
| `GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount...` | fica-document-list.json | List by contract account |

The full `CAInvcgDocument` path is
`/sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001/CAInvcgDocument`
(truncated above for width — see `wiremock/mappings/billing-document-list.json` for the exact
`urlPathPattern`).

---

## Resetting WireMock Between Tests

`@WireMockTest` starts a fresh WireMock server per test class and resets stubs between test
methods automatically — none of the client tests in this project reset manually. Use
`stubFor(...)` (statically imported from `com.github.tomakehurst.wiremock.client.WireMock`)
inside each `@Test` method to register only the stubs that test needs.
