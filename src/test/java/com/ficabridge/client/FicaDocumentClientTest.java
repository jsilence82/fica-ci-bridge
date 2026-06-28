package com.ficabridge.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.odata.ODataFicaDocument;
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
class FicaDocumentClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private FicaDocumentClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        client = new FicaDocumentClient(webClient, OBJECT_MAPPER);
    }

    // ── findByContractAccount ────────────────────────────────────────────────

    @Test
    void findByContractAccount_returnsList() {
        String json = """
                {
                  "value": [
                    {
                      "FiCADocument": "0000001234",
                      "FiCADocumentItem": "0001",
                      "ContractAccount": "0000100200",
                      "BusinessPartner": "0000005678",
                      "DocumentDate": "20240301",
                      "PostingDate": "20240301",
                      "DueDate": "20240401",
                      "ClearingStatus": "0",
                      "Amount": "850.00",
                      "Currency": "EUR",
                      "ConditionType": "ZENE",
                      "FiCADocumentType": "RV"
                    },
                    {
                      "FiCADocument": "0000001235",
                      "FiCADocumentItem": "0001",
                      "ContractAccount": "0000100200",
                      "BusinessPartner": "0000005678",
                      "DocumentDate": "20240201",
                      "ClearingStatus": "1",
                      "Amount": "400.00",
                      "Currency": "EUR"
                    }
                  ]
                }
                """;

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100200'"))
                .willReturn(okJson(json)));

        List<ODataFicaDocument> result = client.findByContractAccount("100200");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFicaDocument()).isEqualTo("0000001234");
        assertThat(result.get(0).getContractAccount()).isEqualTo("0000100200");
        assertThat(result.get(0).getAmount()).isEqualTo("850.00");
        assertThat(result.get(0).getCurrency()).isEqualTo("EUR");
        assertThat(result.get(0).getConditionType()).isEqualTo("ZENE");
        assertThat(result.get(1).getFicaDocument()).isEqualTo("0000001235");
        assertThat(result.get(1).getClearingStatus()).isEqualTo("1");
    }

    @Test
    void findByContractAccount_emptyResult_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .willReturn(okJson("{\"value\":[]}")));

        List<ODataFicaDocument> result = client.findByContractAccount("999");

        assertThat(result).isEmpty();
    }

    @Test
    void findByContractAccount_serverError_throwsODataClientException() {
        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> client.findByContractAccount("100200"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(500));
    }

    // ── findOpenItemsByContractAccount ───────────────────────────────────────

    @Test
    void findOpenItemsByContractAccount_appliesCompoundFilter() {
        String json = """
                {
                  "value": [
                    {
                      "FiCADocument": "0000001234",
                      "ContractAccount": "0000100200",
                      "ClearingStatus": "0",
                      "Amount": "850.00",
                      "Currency": "EUR"
                    }
                  ]
                }
                """;

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100200' and ClearingStatus eq 'OPEN'"))
                .willReturn(okJson(json)));

        List<ODataFicaDocument> result = client.findOpenItemsByContractAccount("100200");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFicaDocument()).isEqualTo("0000001234");
        assertThat(result.get(0).getClearingStatus()).isEqualTo("0");
    }

    @Test
    void findOpenItemsByContractAccount_noOpenItems_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100200' and ClearingStatus eq 'OPEN'"))
                .willReturn(okJson("{\"value\":[]}")));

        List<ODataFicaDocument> result = client.findOpenItemsByContractAccount("100200");

        assertThat(result).isEmpty();
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_v4Response_returnsSingleDocument() {
        String json = """
                {
                  "FiCADocument": "0000001234",
                  "FiCADocumentItem": "0001",
                  "ContractAccount": "0000100200",
                  "BusinessPartner": "0000005678",
                  "DocumentDate": "20240301",
                  "PostingDate": "20240301",
                  "DueDate": "20240401",
                  "ClearingDate": null,
                  "ClearingStatus": "0",
                  "Amount": "850.00",
                  "Currency": "EUR",
                  "ConditionType": "ZENE",
                  "FiCADocumentType": "RV"
                }
                """;

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument('0000001234')"))
                .willReturn(okJson(json)));

        ODataFicaDocument result = client.findById("0000001234");

        assertThat(result).isNotNull();
        assertThat(result.getFicaDocument()).isEqualTo("0000001234");
        assertThat(result.getFicaDocumentItem()).isEqualTo("0001");
        assertThat(result.getBusinessPartner()).isEqualTo("0000005678");
        assertThat(result.getDocumentDate()).isEqualTo("20240301");
        assertThat(result.getDueDate()).isEqualTo("20240401");
        assertThat(result.getClearingStatus()).isEqualTo("0");
        assertThat(result.getAmount()).isEqualTo("850.00");
        assertThat(result.getFicaDocumentType()).isEqualTo("RV");
    }

    @Test
    void findById_v2Envelope_returnsSingleDocument() {
        String json = """
                {
                  "d": {
                    "FiCADocument": "0000009999",
                    "ContractAccount": "0000300400",
                    "Amount": "200.00",
                    "Currency": "USD",
                    "ClearingStatus": "1"
                  }
                }
                """;

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument('0000009999')"))
                .willReturn(okJson(json)));

        ODataFicaDocument result = client.findById("0000009999");

        assertThat(result).isNotNull();
        assertThat(result.getFicaDocument()).isEqualTo("0000009999");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getClearingStatus()).isEqualTo("1");
    }

    @Test
    void findById_notFound_throwsODataClientExceptionWith404() {
        stubFor(get(urlPathMatching("/API_FICADOCUMENT/FiCADocument.*"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.findById("UNKNOWN"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void findById_emptyBody_returnsNull() {
        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument('EMPTY')"))
                .willReturn(ok().withBody("")));

        ODataFicaDocument result = client.findById("EMPTY");

        assertThat(result).isNull();
    }
}
