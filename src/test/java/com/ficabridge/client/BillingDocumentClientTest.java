package com.ficabridge.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.odata.ODataBillingDocument;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class BillingDocumentClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private BillingDocumentClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        client = new BillingDocumentClient(webClient, OBJECT_MAPPER);
    }

    // ── findByContractAccount ────────────────────────────────────────────────

    @Test
    void findByContractAccount_v4Response_returnsList() {
        String json = """
                {
                  "value": [
                    {
                      "BillingDocument": "0090001234",
                      "ContractAccount": "0000100200",
                      "CustomerNumber": "0000005678",
                      "NetAmount": "1250.00",
                      "TransactionCurrency": "EUR",
                      "PaymentDueDate": "20240415",
                      "BillingDocumentIsCancelled": false,
                      "to_BillingDocumentItem": {
                        "value": [
                          {
                            "BillingDocument": "0090001234",
                            "BillingDocumentItem": "000010",
                            "BillingDocumentItemText": "Energy charge",
                            "NetAmount": "980.00",
                            "TaxAmount": "98.00",
                            "TransactionCurrency": "EUR",
                            "ChargingCategory": "ENERGY"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100200'"))
                .withQueryParam("$expand", equalTo("to_BillingDocumentItem"))
                .willReturn(okJson(json)));

        List<ODataBillingDocument> result = client.findByContractAccount("100200");

        assertThat(result).hasSize(1);
        ODataBillingDocument doc = result.get(0);
        assertThat(doc.getBillingDocument()).isEqualTo("0090001234");
        assertThat(doc.getContractAccount()).isEqualTo("0000100200");
        assertThat(doc.getCustomerNumber()).isEqualTo("0000005678");
        assertThat(doc.getNetAmount()).isEqualTo("1250.00");
        assertThat(doc.getTransactionCurrency()).isEqualTo("EUR");
        assertThat(doc.getLineItems()).hasSize(1);
        assertThat(doc.getLineItems().get(0).getBillingDocumentItem()).isEqualTo("000010");
        assertThat(doc.getLineItems().get(0).getChargingCategory()).isEqualTo("ENERGY");
    }

    @Test
    void findByContractAccount_v2Envelope_returnsList() {
        String json = """
                {
                  "d": {
                    "results": [
                      {
                        "BillingDocument": "0090009999",
                        "ContractAccount": "0000200300",
                        "NetAmount": "500.00",
                        "TransactionCurrency": "USD",
                        "BillingDocumentIsCancelled": false,
                        "to_BillingDocumentItem": {
                          "results": []
                        }
                      }
                    ]
                  }
                }
                """;

        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
                .willReturn(okJson(json)));

        List<ODataBillingDocument> result = client.findByContractAccount("200300");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBillingDocument()).isEqualTo("0090009999");
        assertThat(result.get(0).getTransactionCurrency()).isEqualTo("USD");
    }

    @Test
    void findByContractAccount_emptyValueArray_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
                .willReturn(okJson("{\"value\":[]}")));

        List<ODataBillingDocument> result = client.findByContractAccount("100200");

        assertThat(result).isEmpty();
    }

    @Test
    void findByContractAccount_serverError_throwsODataClientException() {
        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
                .willReturn(serverError().withBody("{\"error\":\"Internal Server Error\"}")));

        assertThatThrownBy(() -> client.findByContractAccount("100200"))
                .isInstanceOf(ODataClientException.class)
                .hasMessageContaining("HTTP")
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(500));
    }

    @Test
    void findByContractAccount_unauthorised_throwsODataClientException() {
        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument"))
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
                  "BillingDocument": "0090001234",
                  "ContractAccount": "0000100200",
                  "NetAmount": "1250.00",
                  "TransactionCurrency": "EUR",
                  "PaymentDueDate": "20240415",
                  "ClearingStatus": "1",
                  "BillingDocumentIsCancelled": false,
                  "to_BillingDocumentItem": {
                    "value": [
                      {
                        "BillingDocument": "0090001234",
                        "BillingDocumentItem": "000010",
                        "NetAmount": "980.00",
                        "TransactionCurrency": "EUR"
                      },
                      {
                        "BillingDocument": "0090001234",
                        "BillingDocumentItem": "000020",
                        "NetAmount": "270.00",
                        "TransactionCurrency": "EUR"
                      }
                    ]
                  }
                }
                """;

        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument('0090001234')"))
                .withQueryParam("$expand", equalTo("to_BillingDocumentItem"))
                .willReturn(okJson(json)));

        ODataBillingDocument result = client.findById("0090001234");

        assertThat(result).isNotNull();
        assertThat(result.getBillingDocument()).isEqualTo("0090001234");
        assertThat(result.getClearingStatus()).isEqualTo("1");
        assertThat(result.getLineItems()).hasSize(2);
        assertThat(result.getLineItems().get(1).getBillingDocumentItem()).isEqualTo("000020");
    }

    @Test
    void findById_notFound_throwsODataClientExceptionWith404() {
        stubFor(get(urlPathMatching("/API_BILLING_DOCUMENT_SRV/BillingDocument.*"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.findById("UNKNOWN"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void findById_emptyBody_returnsNull() {
        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument('EMPTY')"))
                .willReturn(ok().withBody("")));

        ODataBillingDocument result = client.findById("EMPTY");

        assertThat(result).isNull();
    }

    @Test
    void findById_cancelledDocument_flagDeserialised() {
        String json = """
                {
                  "BillingDocument": "0090005555",
                  "BillingDocumentIsCancelled": true,
                  "ClearingStatus": null
                }
                """;

        stubFor(get(urlPathEqualTo("/API_BILLING_DOCUMENT_SRV/BillingDocument('0090005555')"))
                .willReturn(okJson(json)));

        ODataBillingDocument result = client.findById("0090005555");

        assertThat(result).isNotNull();
        assertThat(result.getBillingDocumentIsCancelled()).isTrue();
    }
}
