package com.ficabridge.sync;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.odata.ODataFicaDocument;
import com.ficabridge.transformer.FiCaDocTransformer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentSyncSchedulerTest {

    private final DocumentSyncScheduler scheduler =
            new DocumentSyncScheduler(null, null, null, null, new FiCaDocTransformer());

    @Test
    void diff_statusTransitionedToCleared_emitsChange() {
        InvoiceEntity cached = invoice("90001001", "100200", "1234", InvoiceStatus.OPEN, null);

        ODataFicaDocument current = ficaDoc("0000001234", "1", "20240615", "20240101");

        List<DocumentChange> changes = scheduler.diff(List.of(cached), List.of(current));

        assertThat(changes).containsExactly(
                new DocumentChange("90001001", "100200", InvoiceStatus.CLEARED, LocalDate.of(2024, 6, 15)));
    }

    @Test
    void diff_statusUnchanged_emitsNoChange() {
        InvoiceEntity cached = invoice("90001001", "100200", "1234", InvoiceStatus.OPEN, null);

        ODataFicaDocument current = ficaDoc("0000001234", "0", null, "20991231");

        List<DocumentChange> changes = scheduler.diff(List.of(cached), List.of(current));

        assertThat(changes).isEmpty();
    }

    @Test
    void diff_noCachedEntityForFicaDocument_isSkippedWithoutError() {
        InvoiceEntity cached = invoice("90001001", "100200", "1234", InvoiceStatus.OPEN, null);

        ODataFicaDocument current = ficaDoc("0000009999", "1", "20240615", "20240101");

        List<DocumentChange> changes = scheduler.diff(List.of(cached), List.of(current));

        assertThat(changes).isEmpty();
    }

    @Test
    void diff_cachedEntityWithNoLinkedFicaDocument_isSkipped() {
        InvoiceEntity cached = invoice("90001001", "100200", null, InvoiceStatus.OPEN, null);

        ODataFicaDocument current = ficaDoc("0000001234", "1", "20240615", "20240101");

        List<DocumentChange> changes = scheduler.diff(List.of(cached), List.of(current));

        assertThat(changes).isEmpty();
    }

    private static InvoiceEntity invoice(String invoiceNumber, String contractAccount,
            String ficaDocNumber, InvoiceStatus status, LocalDate clearingDate) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceNumber(invoiceNumber);
        entity.setContractAccount(contractAccount);
        entity.setFicaDocNumber(ficaDocNumber);
        entity.setStatus(status);
        entity.setClearingDate(clearingDate);
        return entity;
    }

    private static ODataFicaDocument ficaDoc(
            String ficaDocument, String clearingStatus, String clearingDate, String dueDate) {
        ODataFicaDocument doc = new ODataFicaDocument();
        doc.setFicaDocument(ficaDocument);
        doc.setContractAccount("0000100200");
        doc.setClearingStatus(clearingStatus);
        doc.setClearingDate(clearingDate);
        doc.setDueDate(dueDate);
        return doc;
    }
}
