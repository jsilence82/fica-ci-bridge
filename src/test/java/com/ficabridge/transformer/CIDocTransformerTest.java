package com.ficabridge.transformer;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.odata.ODataCIDocument;
import com.ficabridge.model.odata.ODataCILineItem;
import com.ficabridge.model.odata.ODataWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CIDocTransformerTest {

    private CIDocTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new CIDocTransformer();
    }

    // ── field mapping ────────────────────────────────────────────────────────

    @Test
    void transform_mapsAllScalarFields() {
        ODataCIDocument source = invoicingDoc(
                "000090001234", "0000100200", "0000005678",
                "1250.00", "EUR", LocalDate.of(2024, 4, 15), null);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getInvoiceNumber()).isEqualTo("90001234");
        assertThat(dto.getContractAccount()).isEqualTo("100200");
        assertThat(dto.getBusinessPartner()).isEqualTo("5678");
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2024, 4, 15));
    }

    @Test
    void transform_officialDocumentNumber_isMapped() {
        ODataCIDocument source = invoicingDoc(
                "000090001234", null, null, null, null, null, null);
        source.setCaOfficialDocumentNumber("ODN2024001001");

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getOfficialDocumentNumber()).isEqualTo("ODN2024001001");
    }

    @Test
    void transform_nullOptionalFields_areNull() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, null, null);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isNull();
        assertThat(dto.getBusinessPartner()).isNull();
        assertThat(dto.getAmount()).isNull();
        assertThat(dto.getCurrency()).isNull();
        assertThat(dto.getDueDate()).isNull();
        assertThat(dto.getOfficialDocumentNumber()).isNull();
        assertThat(dto.getLineItems()).isNull();
    }

    // ── status derivation ────────────────────────────────────────────────────

    @Test
    void transform_reversalDocumentPresent_isReversed() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, LocalDate.now().plusDays(30), null);
        source.setCaInvcgReversalDocument("000090009999");

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.REVERSED);
    }

    @Test
    void transform_allItemsCleared_isCleared() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, LocalDate.now().plusDays(10), null);
        source.setExpandedItems(wrapItems(
                lineItem("00000001", "980.00", "EUR", "USAG", "CLEAR001"),
                lineItem("00000002", "270.00", "EUR", "FIXD", "CLEAR002")));

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.CLEARED);
    }

    @Test
    void transform_someItemsCleared_isPartiallyPaid() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, LocalDate.now().plusDays(10), null);
        source.setExpandedItems(wrapItems(
                lineItem("00000001", "980.00", "EUR", "USAG", "CLEAR001"),
                lineItem("00000002", "270.00", "EUR", "FIXD", null)));

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void transform_notClearedFutureDueDate_isOpen() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, LocalDate.now().plusDays(30), null);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void transform_notClearedPastDueDate_isOverdue() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, LocalDate.now().minusDays(1), null);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    void transform_notClearedNullDueDate_isOpen() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, null, null);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void transform_reversalTakesPrecedenceOverClearing() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, null, null);
        source.setCaInvcgReversalDocument("000090009999");
        source.setExpandedItems(wrapItems(
                lineItem("00000001", "980.00", "EUR", "USAG", "CLEAR001")));

        // Reversal flag wins even if all items are cleared
        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.REVERSED);
    }

    // ── line items ───────────────────────────────────────────────────────────

    @Test
    void transform_withLineItems_mapsItems() {
        ODataCIDocument source = invoicingDoc(
                "000090001234", null, null, null, "EUR", null, null);

        ODataCILineItem item = new ODataCILineItem();
        item.setCaInvoicingDocument("000090001234");
        item.setCaInvcgDocItem("00000010");
        item.setCaMainTransaction("0100");
        item.setCaSubTransaction("0100");
        item.setQuantity("100.000");
        item.setUnitOfMeasure("KWH");
        item.setCaAmountInTransactionCurrency("980.00");
        item.setCaTaxAmountInTransCurrency("98.00");
        item.setTransactionCurrency("EUR");
        item.setCaConditionType("USAG");
        item.setCaClearingDocumentNumber("");

        source.setExpandedItems(wrapItems(item));

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getLineItems()).hasSize(1);
        var li = dto.getLineItems().get(0);
        assertThat(li.getItemNumber()).isEqualTo("10");
        assertThat(li.getQuantity()).isEqualByComparingTo(new BigDecimal("100.000"));
        assertThat(li.getQuantityUnit()).isEqualTo("KWH");
        assertThat(li.getNetAmount()).isEqualByComparingTo(new BigDecimal("980.00"));
        assertThat(li.getTaxAmount()).isEqualByComparingTo(new BigDecimal("98.00"));
        assertThat(li.getCurrency()).isEqualTo("EUR");
        assertThat(li.getChargingCategory()).isEqualTo("USAG");
    }

    @Test
    void transform_ficaDocNumber_linkedFromLineItemCADocumentNumber() {
        // The invoicing document (API_CAINVOICINGDOCUMENT) carries the linked FI-CA accounting
        // document number (OPBEL) on its items as CADocumentNumber; the transformer lifts it onto
        // the invoice so document-sync can match FI-CA clearing updates back to this invoice.
        ODataCIDocument source = invoicingDoc(
                "000090001234", null, null, null, "EUR", null, null);

        ODataCILineItem item1 = lineItem("00000001", "980.00", "EUR", "USAG", "");
        item1.setCaDocumentNumber("0000004711");
        ODataCILineItem item2 = lineItem("00000002", "270.00", "EUR", "FIXD", "");
        item2.setCaDocumentNumber("0000004711");
        source.setExpandedItems(wrapItems(item1, item2));

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getFicaDocNumber()).isEqualTo("4711");
    }

    @Test
    void transform_noLineItemCADocumentNumber_ficaDocNumberIsNull() {
        ODataCIDocument source = invoicingDoc(
                "000090001234", null, null, null, "EUR", null, null);
        // line item present but with no linked FI-CA document number
        source.setExpandedItems(wrapItems(lineItem("00000001", "980.00", "EUR", "USAG", "")));

        assertThat(transformer.transform(source).getFicaDocNumber()).isNull();
    }

    @Test
    void transform_emptyLineItems_lineItemsIsNull() {
        ODataCIDocument source = invoicingDoc(
                "0000000001", null, null, null, null, null, null);
        // no wrapper set — getLineItems() returns List.of()

        InvoiceDTO dto = transformer.transform(source);
        assertThat(dto.getLineItems()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ODataCIDocument invoicingDoc(String docNum, String contractAccount,
            String businessPartner, String amount, String currency,
            LocalDate dueDate, String officialDocNum) {
        ODataCIDocument doc = new ODataCIDocument();
        doc.setCaInvoicingDocument(docNum);
        doc.setContractAccount(contractAccount);
        doc.setBusinessPartner(businessPartner);
        doc.setCaAmountInTransactionCurrency(amount);
        doc.setTransactionCurrency(currency);
        doc.setCaNetDueDate(dueDate);
        doc.setCaOfficialDocumentNumber(officialDocNum);
        return doc;
    }

    private ODataCILineItem lineItem(String itemNum, String amount, String currency,
            String conditionType, String clearingDocNum) {
        ODataCILineItem item = new ODataCILineItem();
        item.setCaInvcgDocItem(itemNum);
        item.setCaAmountInTransactionCurrency(amount);
        item.setTransactionCurrency(currency);
        item.setCaConditionType(conditionType);
        item.setCaClearingDocumentNumber(clearingDocNum);
        return item;
    }

    @SafeVarargs
    private <T> ODataWrapper<T> wrapItems(T... items) {
        ODataWrapper<T> wrapper = new ODataWrapper<>();
        wrapper.setValue(List.of(items));
        return wrapper;
    }
}
