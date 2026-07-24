package com.ficabridge.sync;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaDocumentChangeIngesterTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private JpaDocumentChangeIngester ingester;

    @BeforeEach
    void setUp() {
        ingester = new JpaDocumentChangeIngester(invoiceRepository);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceNumber("90001001");
        entity.setBusinessPartner("5678");
        entity.setContractAccount("100200");
        entity.setFicaDocNumber("1234");
        entity.setAmount(new BigDecimal("143.40"));
        entity.setCurrency("EUR");
        entity.setStatus(InvoiceStatus.OPEN);
        invoiceRepository.save(entity);
    }

    @Test
    void ingest_appliesChangeToCachedEntity() {
        ingester.ingest(List.of(
                new DocumentChange("90001001", "100200", InvoiceStatus.CLEARED, LocalDate.of(2024, 6, 15))));

        InvoiceEntity updated = invoiceRepository.findByInvoiceNumber("90001001").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.CLEARED);
        assertThat(updated.getClearingDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(updated.getLastSyncedAt()).isNotNull();
    }

    @Test
    void ingest_sameDocumentTwice_updatesRatherThanDuplicates() {
        DocumentChange change =
                new DocumentChange("90001001", "100200", InvoiceStatus.CLEARED, LocalDate.of(2024, 6, 15));

        ingester.ingest(List.of(change));
        ingester.ingest(List.of(change));

        assertThat(invoiceRepository.findAll()).hasSize(1);
        assertThat(invoiceRepository.findByInvoiceNumber("90001001").orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.CLEARED);
    }

    @Test
    void ingest_unknownInvoiceNumber_isSkippedWithoutInserting() {
        DocumentChange change = new DocumentChange("UNKNOWN", "100200", InvoiceStatus.CLEARED, LocalDate.now());

        ingester.ingest(List.of(change));

        // skipped, not inserted — the cache still holds only the seeded invoice
        assertThat(invoiceRepository.findAll()).hasSize(1);
        assertThat(invoiceRepository.findByInvoiceNumber("UNKNOWN")).isEmpty();
    }

    @Test
    void ingest_unknownDocInBatch_doesNotPoisonLegitimateTransitions() {
        DocumentChange known = new DocumentChange("90001001", "100200", InvoiceStatus.CLEARED, LocalDate.of(2024, 6, 15));
        DocumentChange unknown = new DocumentChange("UNKNOWN", "100200", InvoiceStatus.CLEARED, LocalDate.now());

        // unknown is listed first, so a throw-on-miss would roll back the known transition too
        ingester.ingest(List.of(unknown, known));

        InvoiceEntity updated = invoiceRepository.findByInvoiceNumber("90001001").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.CLEARED);
        assertThat(updated.getClearingDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(invoiceRepository.findByInvoiceNumber("UNKNOWN")).isEmpty();
    }
}
