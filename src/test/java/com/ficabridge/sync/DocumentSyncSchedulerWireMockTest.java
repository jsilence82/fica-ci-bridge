package com.ficabridge.sync;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ficabridge.client.FicaDocumentClient;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.entity.SyncRunEntity;
import com.ficabridge.repository.InvoiceRepository;
import com.ficabridge.repository.SyncRunRepository;
import com.ficabridge.transformer.FiCaDocTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link DocumentSyncScheduler#syncOpenDocuments()} against a real
 * {@link FicaDocumentClient} pointed at WireMock, with the repositories and ingester mocked —
 * the client boundary is the thing worth stubbing over HTTP; the rest is plain collaboration.
 */
@WireMockTest
class DocumentSyncSchedulerWireMockTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private InvoiceRepository invoiceRepository;
    private SyncRunRepository syncRunRepository;
    private DocumentChangeIngester ingester;
    private DocumentSyncScheduler scheduler;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        WebClient webClient = WebClient.builder().baseUrl(wmRuntimeInfo.getHttpBaseUrl()).build();
        FicaDocumentClient ficaDocumentClient =
                new FicaDocumentClient(webClient, OBJECT_MAPPER, RateLimiter.ofDefaults("test"));

        invoiceRepository = mock(InvoiceRepository.class);
        syncRunRepository = mock(SyncRunRepository.class);
        ingester = mock(DocumentChangeIngester.class);

        scheduler = new DocumentSyncScheduler(
                invoiceRepository, syncRunRepository, ficaDocumentClient, ingester, new FiCaDocTransformer());
    }

    @Test
    void syncOpenDocuments_oneContractAccountUnreachable_recordsFailedRunButIngestsTheRest() {
        InvoiceEntity failingAccountInvoice = invoice("90001001", "100", "1111", InvoiceStatus.OPEN);
        InvoiceEntity healthyAccountInvoice = invoice("90002002", "200", "2222", InvoiceStatus.OPEN);
        when(invoiceRepository.findByStatusIn(any()))
                .thenReturn(List.of(failingAccountInvoice, healthyAccountInvoice));

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '100'"))
                .willReturn(serverError()));

        stubFor(get(urlPathEqualTo("/API_FICADOCUMENT/FiCADocument"))
                .withQueryParam("$filter", equalTo("ContractAccount eq '200'"))
                .willReturn(okJson("""
                        {
                          "value": [
                            {
                              "FiCADocument": "0000002222",
                              "ContractAccount": "0000000200",
                              "ClearingStatus": "1",
                              "ClearingDate": "20240701",
                              "Amount": "400.00",
                              "Currency": "EUR"
                            }
                          ]
                        }
                        """)));

        assertThatCode(() -> scheduler.syncOpenDocuments()).doesNotThrowAnyException();

        ArgumentCaptor<List<DocumentChange>> changesCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingester).ingest(changesCaptor.capture());
        assertThat(changesCaptor.getValue()).containsExactly(
                new DocumentChange("90002002", "200", InvoiceStatus.CLEARED, LocalDate.of(2024, 7, 1)));

        ArgumentCaptor<SyncRunEntity> runCaptor = ArgumentCaptor.forClass(SyncRunEntity.class);
        verify(syncRunRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo(SyncRunEntity.Status.FAILED);
        assertThat(runCaptor.getValue().getFailureReason()).isNotNull();
    }

    private static InvoiceEntity invoice(
            String invoiceNumber, String contractAccount, String ficaDocNumber, InvoiceStatus status) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceNumber(invoiceNumber);
        entity.setContractAccount(contractAccount);
        entity.setFicaDocNumber(ficaDocNumber);
        entity.setStatus(status);
        return entity;
    }
}
