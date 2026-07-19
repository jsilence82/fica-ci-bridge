package com.ficabridge.sync;

import com.ficabridge.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class JpaDocumentChangeIngesterTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    private JpaDocumentChangeIngester ingester;

    @BeforeEach
    void setUp() {
        ingester = new JpaDocumentChangeIngester(invoiceRepository);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setIdocDocnum("IDOC001");
        entity.setBillingDocNumber("90001001");
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

        InvoiceEntity updated = invoiceRepository.findByBillingDocNumber("90001001").orElseThrow();
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
        assertThat(invoiceRepository.findByBillingDocNumber("90001001").orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.CLEARED);
    }

    @Test
    void ingest_unknownBillingDocNumber_throwsResourceNotFoundException() {
        DocumentChange change = new DocumentChange("UNKNOWN", "100200", InvoiceStatus.CLEARED, LocalDate.now());

        assertThatThrownBy(() -> ingester.ingest(List.of(change)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
