package com.ficabridge.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.odata.ODataCIDocument;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class CIDocumentClientTest {

    private static final String BASE_PATH =
            "/sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001/CAInvcgDocument";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private CIDocumentClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        // Effectively unlimited — rate-limiter engagement is covered separately in ODataClientRateLimiterTest.
        client = new CIDocumentClient(webClient, OBJECT_MAPPER, RateLimiter.ofDefaults("test"));
    }

    // ── findByContractAccount ────────────────────────────────────────────────

    @Test
    void findByContractAccount_v4Response_returnsList() {
        String json = """
                {
                  "value": [
                    {
                      "CAInvoicingDocument": "000090001234",
                      "ContractAccount": "0000100200",
                      "BusinessPartner": "0000005678",
                      "CAAmountInTransactionCurrency": "1250.00",
                      "TransactionCurrency": "EUR",
                      "CANetDueDate": "2024-04-15",
                      "CAOfficialDocumentNumber": "ODN2024001234",
                      "CAInvcgReversalDocument": "",
                      "_CAInvcgDocItem": {
                        "value": [
                          {
                            "CAInvoicingDocument": "000090001234",
                            "CAInvcgDocItem": "00000001",
                            "CAAmountInTransactionCurrency": "980.00",
                            "CATaxAmountInTransCurrency": "98.00",
                            "TransactionCurrency": "EUR",
                            "CAConditionType": "USAG",
                            "CAClearingDocumentNumber": ""
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        stubFor(get(urlPathEqualTo(BASE_PATH))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100200'"))
                .withQueryParam("$expand", equalTo("_CAInvcgDocItem"))
                .willReturn(okJson(json)));

        List<ODataCIDocument> result = client.findByContractAccount("100200");

        assertThat(result).hasSize(1);
        ODataCIDocument doc = result.get(0);
        assertThat(doc.getCaInvoicingDocument()).isEqualTo("000090001234");
        assertThat(doc.getContractAccount()).isEqualTo("0000100200");
        assertThat(doc.getBusinessPartner()).isEqualTo("0000005678");
        assertThat(doc.getCaAmountInTransactionCurrency()).isEqualTo("1250.00");
        assertThat(doc.getTransactionCurrency()).isEqualTo("EUR");
        assertThat(doc.getCaOfficialDocumentNumber()).isEqualTo("ODN2024001234");
        assertThat(doc.getLineItems()).hasSize(1);
        assertThat(doc.getLineItems().get(0).getCaInvcgDocItem()).isEqualTo("00000001");
        assertThat(doc.getLineItems().get(0).getCaConditionType()).isEqualTo("USAG");
    }

    @Test
    void findByContractAccount_v2Envelope_returnsList() {
        String json = """
                {
                  "d": {
                    "results": [
                      {
                        "CAInvoicingDocument": "000090009999",
                        "ContractAccount": "0000200300",
                        "CAAmountInTransactionCurrency": "500.00",
                        "TransactionCurrency": "USD"
                      }
                    ]
                  }
                }
                """;

        stubFor(get(urlPathEqualTo(BASE_PATH))
                .willReturn(okJson(json)));

        List<ODataCIDocument> result = client.findByContractAccount("200300");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCaInvoicingDocument()).isEqualTo("000090009999");
        assertThat(result.get(0).getTransactionCurrency()).isEqualTo("USD");
    }

    @Test
    void findByContractAccount_emptyValueArray_returnsEmptyList() {
        stubFor(get(urlPathEqualTo(BASE_PATH))
                .willReturn(okJson("{\"value\":[]}")));

        List<ODataCIDocument> result = client.findByContractAccount("100200");

        assertThat(result).isEmpty();
    }

    @Test
    void findByContractAccount_serverError_throwsODataClientException() {
        stubFor(get(urlPathEqualTo(BASE_PATH))
                .willReturn(serverError().withBody("{\"error\":\"Internal Server Error\"}")));

        assertThatThrownBy(() -> client.findByContractAccount("100200"))
                .isInstanceOf(ODataClientException.class)
                .hasMessageContaining("HTTP")
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(500));
    }

    @Test
    void findByContractAccount_unauthorised_throwsODataClientException() {
        stubFor(get(urlPathEqualTo(BASE_PATH))
                .willReturn(unauthorized()));

        assertThatThrownBy(() -> client.findByContractAccount("100200"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(401));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_v4Response_returnsDocumentWithLineItems() {
        String json = """
                {
                  "CAInvoicingDocument": "000090001234",
                  "ContractAccount": "0000100200",
                  "CAAmountInTransactionCurrency": "1250.00",
                  "TransactionCurrency": "EUR",
                  "CANetDueDate": "2024-04-15",
                  "CAOfficialDocumentNumber": "ODN2024001234",
                  "CAInvcgReversalDocument": "",
                  "_CAInvcgDocItem": {
                    "value": [
                      {
                        "CAInvoicingDocument": "000090001234",
                        "CAInvcgDocItem": "00000001",
                        "CAAmountInTransactionCurrency": "980.00",
                        "TransactionCurrency": "EUR",
                        "CAClearingDocumentNumber": "CLRNG001"
                      },
                      {
                        "CAInvoicingDocument": "000090001234",
                        "CAInvcgDocItem": "00000002",
                        "CAAmountInTransactionCurrency": "270.00",
                        "TransactionCurrency": "EUR",
                        "CAClearingDocumentNumber": "CLRNG002"
                      }
                    ]
                  }
                }
                """;

        stubFor(get(urlPathEqualTo(BASE_PATH + "(CAInvoicingDocument='000090001234')"))
                .withQueryParam("$expand", equalTo("_CAInvcgDocItem"))
                .willReturn(okJson(json)));

        ODataCIDocument result = client.findById("000090001234");

        assertThat(result).isNotNull();
        assertThat(result.getCaInvoicingDocument()).isEqualTo("000090001234");
        assertThat(result.getCaOfficialDocumentNumber()).isEqualTo("ODN2024001234");
        assertThat(result.getLineItems()).hasSize(2);
        assertThat(result.getLineItems().get(1).getCaInvcgDocItem()).isEqualTo("00000002");
        assertThat(result.getLineItems().get(1).getCaClearingDocumentNumber()).isEqualTo("CLRNG002");
    }

    @Test
    void findById_notFound_throwsODataClientExceptionWith404() {
        stubFor(get(urlPathMatching(BASE_PATH + ".*"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.findById("UNKNOWN"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void findById_emptyBody_returnsNull() {
        stubFor(get(urlPathEqualTo(BASE_PATH + "(CAInvoicingDocument='EMPTY')"))
                .willReturn(ok().withBody("")));

        ODataCIDocument result = client.findById("EMPTY");

        assertThat(result).isNull();
    }

    @Test
    void findById_reversalDocument_fieldDeserialised() {
        String json = """
                {
                  "CAInvoicingDocument": "000090005555",
                  "CAInvcgReversalDocument": "000090006666",
                  "CAInvcgReversedDocument": null
                }
                """;

        stubFor(get(urlPathEqualTo(BASE_PATH + "(CAInvoicingDocument='000090005555')"))
                .willReturn(okJson(json)));

        ODataCIDocument result = client.findById("000090005555");

        assertThat(result).isNotNull();
        assertThat(result.getCaInvcgReversalDocument()).isEqualTo("000090006666");
        assertThat(result.getCaInvcgReversedDocument()).isNull();
    }
}
