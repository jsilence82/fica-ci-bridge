package com.ficabridge.transformer;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.odata.ODataBillingDocument;
import com.ficabridge.model.odata.ODataBillingLineItem;
import com.ficabridge.model.odata.ODataWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BillingDocTransformerTest {

    private BillingDocTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new BillingDocTransformer();
    }

    // ── field mapping ────────────────────────────────────────────────────────

    @Test
    void transform_mapsAllScalarFields() {
        ODataBillingDocument source = billingDoc("0090001234", "0000100200", "0000005678",
                "1250.00", "EUR", "20240315", "20240415", null, false);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getBillingDocNumber()).isEqualTo("90001234");
        assertThat(dto.getContractAccount()).isEqualTo("100200");
        assertThat(dto.getBusinessPartner()).isEqualTo("5678");
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2024, 4, 15));
        assertThat(dto.getClearingDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void transform_nullOptionalFields_areNull() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, null, false);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getContractAccount()).isNull();
        assertThat(dto.getBusinessPartner()).isNull();
        assertThat(dto.getAmount()).isNull();
        assertThat(dto.getCurrency()).isNull();
        assertThat(dto.getDueDate()).isNull();
        assertThat(dto.getClearingDate()).isNull();
        assertThat(dto.getLineItems()).isNull();
    }

    @Test
    void transform_zeroDate_producesNullDate() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, "00000000", "00000000", null, false);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getClearingDate()).isNull();
        assertThat(dto.getDueDate()).isNull();
    }

    // ── status derivation ────────────────────────────────────────────────────

    @Test
    void transform_cancelledDoc_isReversed() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, null, true);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.REVERSED);
    }

    @Test
    void transform_clearingStatusOne_isCleared() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, "1", false);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.CLEARED);
    }

    @Test
    void transform_clearingStatusTwo_isPartiallyPaid() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, "2", false);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
    }

    @Test
    void transform_notClearedFutureDueDate_isOpen() {
        String futureDate = LocalDate.now().plusDays(30).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, futureDate, null, false);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void transform_notClearedPastDueDate_isOverdue() {
        String pastDate = LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, pastDate, null, false);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    void transform_notClearedNullDueDate_isOpen() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, null, false);

        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void transform_cancelledTakesPrecedenceOverClearingStatus() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, "1", true);

        // Cancelled flag wins even if clearing status says CLEARED
        assertThat(transformer.transform(source).getStatus()).isEqualTo(InvoiceStatus.REVERSED);
    }

    // ── line items ───────────────────────────────────────────────────────────

    @Test
    void transform_withLineItems_mapsItems() {
        ODataBillingDocument source = billingDoc("0090001234", null, null,
                null, "EUR", null, null, null, false);

        ODataBillingLineItem item = new ODataBillingLineItem();
        item.setBillingDocument("0090001234");
        item.setBillingDocumentItem("000010");
        item.setBillingDocumentItemText("  Energy charge  ");
        item.setMaterial("MAT001");
        item.setBillingQuantity("100.000");
        item.setBillingQuantityUnit("KWH");
        item.setNetAmount("980.00");
        item.setTaxAmount("98.00");
        item.setTransactionCurrency("EUR");
        item.setChargingCategory("ENERGY");

        ODataWrapper<ODataBillingLineItem> wrapper = new ODataWrapper<>();
        wrapper.setValue(List.of(item));
        source.setToLineItems(wrapper);

        InvoiceDTO dto = transformer.transform(source);

        assertThat(dto.getLineItems()).hasSize(1);
        var li = dto.getLineItems().get(0);
        assertThat(li.getItemNumber()).isEqualTo("10");
        assertThat(li.getDescription()).isEqualTo("Energy charge");
        assertThat(li.getMaterial()).isEqualTo("MAT001");
        assertThat(li.getQuantity()).isEqualByComparingTo(new BigDecimal("100.000"));
        assertThat(li.getQuantityUnit()).isEqualTo("KWH");
        assertThat(li.getNetAmount()).isEqualByComparingTo(new BigDecimal("980.00"));
        assertThat(li.getTaxAmount()).isEqualByComparingTo(new BigDecimal("98.00"));
        assertThat(li.getCurrency()).isEqualTo("EUR");
        assertThat(li.getChargingCategory()).isEqualTo("ENERGY");
    }

    @Test
    void transform_emptyLineItems_lineItemsIsNull() {
        ODataBillingDocument source = billingDoc("0000000001", null, null,
                null, null, null, null, null, false);
        // no wrapper set — getLineItems() returns List.of()

        InvoiceDTO dto = transformer.transform(source);
        assertThat(dto.getLineItems()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ODataBillingDocument billingDoc(String docNum, String contractAccount,
            String customerNumber, String netAmount, String currency,
            String clearingDate, String paymentDueDate, String clearingStatus,
            boolean cancelled) {
        ODataBillingDocument doc = new ODataBillingDocument();
        doc.setBillingDocument(docNum);
        doc.setContractAccount(contractAccount);
        doc.setCustomerNumber(customerNumber);
        doc.setNetAmount(netAmount);
        doc.setTransactionCurrency(currency);
        doc.setClearingDate(clearingDate);
        doc.setPaymentDueDate(paymentDueDate);
        doc.setClearingStatus(clearingStatus);
        doc.setBillingDocumentIsCancelled(cancelled);
        return doc;
    }
}
