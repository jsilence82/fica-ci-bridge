package com.ficabridge.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.odata.ODataContractAccount;
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
class ContractAccountClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private ContractAccountClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder()
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .build();
        // Effectively unlimited — rate-limiter engagement is covered separately in ODataClientRateLimiterTest.
        client = new ContractAccountClient(webClient, OBJECT_MAPPER, RateLimiter.ofDefaults("test"));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_v4Response_returnsSingleAccount() {
        String json = """
                {
                  "ContractAccount": "0000100200",
                  "ContractAccountName": "Test Corp",
                  "BusinessPartner": "0000005678",
                  "ContractAccountCategory": "01",
                  "IncomingPaymentMethod": "D",
                  "DunningProcedure": "Z001"
                }
                """;

        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount('0000100200')"))
                .willReturn(okJson(json)));

        ODataContractAccount result = client.findById("0000100200");

        assertThat(result).isNotNull();
        assertThat(result.getContractAccount()).isEqualTo("0000100200");
        assertThat(result.getContractAccountName()).isEqualTo("Test Corp");
        assertThat(result.getBusinessPartner()).isEqualTo("0000005678");
        assertThat(result.getContractAccountCategory()).isEqualTo("01");
        assertThat(result.getIncomingPaymentMethod()).isEqualTo("D");
        assertThat(result.getDunningProcedure()).isEqualTo("Z001");
    }

    @Test
    void findById_v2Envelope_returnsSingleAccount() {
        // OData V2 wraps the single entity in { "d": { ... } }
        String json = """
                {
                  "d": {
                    "ContractAccount": "0000100200",
                    "ContractAccountName": "Test Corp V2",
                    "BusinessPartner": "0000005678",
                    "OutgoingPaymentMethod": "C",
                    "PaymentCondition": "0001"
                  }
                }
                """;

        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount('0000100200')"))
                .willReturn(okJson(json)));

        ODataContractAccount result = client.findById("0000100200");

        assertThat(result).isNotNull();
        assertThat(result.getContractAccount()).isEqualTo("0000100200");
        assertThat(result.getContractAccountName()).isEqualTo("Test Corp V2");
        assertThat(result.getOutgoingPaymentMethod()).isEqualTo("C");
        assertThat(result.getPaymentCondition()).isEqualTo("0001");
    }

    @Test
    void findById_notFound_throwsODataClientExceptionWith404() {
        stubFor(get(urlPathMatching("/API_CA_CONTRACTACCOUNT/ContractAccount.*"))
                .willReturn(notFound()));

        assertThatThrownBy(() -> client.findById("UNKNOWN"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(404));
    }

    @Test
    void findById_emptyBody_returnsNull() {
        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount('EMPTY')"))
                .willReturn(ok().withBody("")));

        ODataContractAccount result = client.findById("EMPTY");

        assertThat(result).isNull();
    }

    @Test
    void findById_serverError_throwsODataClientException() {
        stubFor(get(urlPathMatching("/API_CA_CONTRACTACCOUNT/ContractAccount.*"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> client.findById("9999"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(500));
    }

    // ── findByBusinessPartner ────────────────────────────────────────────────

    @Test
    void findByBusinessPartner_returnsList() {
        String json = """
                {
                  "value": [
                    {
                      "ContractAccount": "0000100200",
                      "BusinessPartner": "0000005678",
                      "ContractAccountCategory": "01"
                    },
                    {
                      "ContractAccount": "0000100201",
                      "BusinessPartner": "0000005678",
                      "ContractAccountCategory": "02"
                    }
                  ]
                }
                """;

        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount"))
                .withQueryParam("$filter", equalTo("BusinessPartner eq '5678'"))
                .willReturn(okJson(json)));

        List<ODataContractAccount> result = client.findByBusinessPartner("5678");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContractAccount()).isEqualTo("0000100200");
        assertThat(result.get(1).getContractAccount()).isEqualTo("0000100201");
        assertThat(result.get(1).getContractAccountCategory()).isEqualTo("02");
    }

    @Test
    void findByBusinessPartner_v2Envelope_returnsList() {
        String json = """
                {
                  "d": {
                    "results": [
                      {
                        "ContractAccount": "0000300400",
                        "BusinessPartner": "0000009999"
                      }
                    ]
                  }
                }
                """;

        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount"))
                .willReturn(okJson(json)));

        List<ODataContractAccount> result = client.findByBusinessPartner("9999");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContractAccount()).isEqualTo("0000300400");
    }

    @Test
    void findByBusinessPartner_emptyResponse_returnsEmptyList() {
        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount"))
                .willReturn(okJson("{\"value\":[]}")));

        List<ODataContractAccount> result = client.findByBusinessPartner("NONE");

        assertThat(result).isEmpty();
    }

    @Test
    void findByBusinessPartner_serverError_throwsODataClientException() {
        stubFor(get(urlPathEqualTo("/API_CA_CONTRACTACCOUNT/ContractAccount"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> client.findByBusinessPartner("5678"))
                .isInstanceOf(ODataClientException.class)
                .satisfies(ex -> assertThat(((ODataClientException) ex).getHttpStatus()).isEqualTo(500));
    }
}
