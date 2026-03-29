package com.ficabridge.transformer;

import com.ficabridge.model.dto.FicaDocumentDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.odata.ODataFicaDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class FiCaDocTransformerTest {

    private static final DateTimeFormatter SAP_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FiCaDocTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new FiCaDocTransformer();
    }

    // ── field mapping ────────────────────────────────────────────────────────

    @Test
    void transform_mapsAllScalarFields() {
        ODataFicaDocument source = ficaDoc(
                "0000090001", "000010",
                "0000100200", "0000005678",
                "20240101", "20240102", "20240315", "20240320",
                "1", "750.50", "USD", "E001", "ZR"
        );

        FicaDocumentDTO dto = transformer.transform(source);

        assertThat(dto.getDocumentNumber()).isEqualTo("90001");
        assertThat(dto.getItemNumber()).isEqualTo("10");
        assertThat(dto.getContractAccount()).isEqualTo("100200");
        assertThat(dto.getBusinessPartner()).isEqualTo("5678");
        assertThat(dto.getDocumentDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(dto.getPostingDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(dto.getClearingDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("750.50"));
        assertThat(dto.getCurrency()).isEqualTo("USD");
        assertThat(dto.getConditionType()).isEqualTo("E001");
        assertThat(dto.getDocumentType()).isEqualTo("ZR");
    }

    @Test
    void transform_nullOptionalFields_areNull() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );

        FicaDocumentDTO dto = transformer.transform(source);

        assertThat(dto.getItemNumber()).isNull();
        assertThat(dto.getContractAccount()).isNull();
        assertThat(dto.getBusinessPartner()).isNull();
        assertThat(dto.getDocumentDate()).isNull();
        assertThat(dto.getPostingDate()).isNull();
        assertThat(dto.getDueDate()).isNull();
        assertThat(dto.getClearingDate()).isNull();
        assertThat(dto.getAmount()).isNull();
        assertThat(dto.getCurrency()).isNull();
    }

    @Test
    void transform_zeroDate_producesNullDate() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                "00000000", "00000000", "00000000", "00000000",
                null, null, null, null, null
        );

        FicaDocumentDTO dto = transformer.transform(source);

        assertThat(dto.getDocumentDate()).isNull();
        assertThat(dto.getPostingDate()).isNull();
        assertThat(dto.getDueDate()).isNull();
        assertThat(dto.getClearingDate()).isNull();
    }

    @Test
    void transform_trailingSpacesInStringFields_areTrimmed() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, null, null,
                null, null, "  EUR  ", "  KSCHL  ", "  ZR  "
        );

        FicaDocumentDTO dto = transformer.transform(source);

        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getConditionType()).isEqualTo("KSCHL");
        assertThat(dto.getDocumentType()).isEqualTo("ZR");
    }

    // ── status derivation ────────────────────────────────────────────────────

    @Test
    void transform_clearingStatusOne_isCleared() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, null, null,
                "1", null, null, null, null
        );

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.CLEARED);
    }

    @Test
    void transform_clearingStatusTwo_isPartiallyPaid() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, null, null,
                "2", null, null, null, null
        );

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void transform_notClearedPastDueDate_isOverdue() {
        String pastDate = LocalDate.now().minusDays(1).format(SAP_DATE);
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, pastDate, null,
                null, null, null, null, null
        );

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    void transform_notClearedFutureDueDate_isOpen() {
        String futureDate = LocalDate.now().plusDays(10).format(SAP_DATE);
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, futureDate, null,
                null, null, null, null, null
        );

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void transform_notClearedNullDueDate_isOpen() {
        ODataFicaDocument source = ficaDoc(
                "0000000001", null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ODataFicaDocument ficaDoc(
            String docNum, String itemNum,
            String contractAccount, String businessPartner,
            String documentDate, String postingDate, String dueDate, String clearingDate,
            String clearingStatus, String amount, String currency,
            String conditionType, String documentType) {
        ODataFicaDocument doc = new ODataFicaDocument();
        doc.setFicaDocument(docNum);
        doc.setFicaDocumentItem(itemNum);
        doc.setContractAccount(contractAccount);
        doc.setBusinessPartner(businessPartner);
        doc.setDocumentDate(documentDate);
        doc.setPostingDate(postingDate);
        doc.setDueDate(dueDate);
        doc.setClearingDate(clearingDate);
        doc.setClearingStatus(clearingStatus);
        doc.setAmount(amount);
        doc.setCurrency(currency);
        doc.setConditionType(conditionType);
        doc.setFicaDocumentType(documentType);
        return doc;
    }
}
