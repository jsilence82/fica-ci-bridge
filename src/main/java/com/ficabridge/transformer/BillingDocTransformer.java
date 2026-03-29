package com.ficabridge.transformer;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceLineItemDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.odata.ODataBillingDocument;
import com.ficabridge.model.odata.ODataBillingLineItem;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Transforms a Convergent Invoicing billing OData response into the application's domain model.
 * Handles SAP clearing-status derivation, OVERDUE detection, cancellation, and zero-padding.
 */
@Component
public class BillingDocTransformer {

    /**
     * Map a single OData billing document to an {@link InvoiceDTO}.
     * Line items are included when the OData response was fetched with
     * {@code $expand=to_BillingDocumentItem}.
     */
    public InvoiceDTO transform(ODataBillingDocument source) {
        InvoiceDTO dto = new InvoiceDTO();

        dto.setBillingDocNumber(TransformerUtils.stripLeadingZeros(source.getBillingDocument()));
        dto.setContractAccount(TransformerUtils.stripLeadingZeros(source.getContractAccount()));
        dto.setBusinessPartner(TransformerUtils.stripLeadingZeros(source.getCustomerNumber()));
        dto.setDueDate(TransformerUtils.parseSapDate(source.getPaymentDueDate()));
        dto.setClearingDate(TransformerUtils.parseSapDate(source.getClearingDate()));
        dto.setAmount(TransformerUtils.parseSapAmount(source.getNetAmount()));
        dto.setCurrency(TransformerUtils.trimSapString(source.getTransactionCurrency()));
        dto.setStatus(deriveStatus(source));

        List<ODataBillingLineItem> rawItems = source.getLineItems();
        if (rawItems != null && !rawItems.isEmpty()) {
            dto.setLineItems(rawItems.stream().map(this::transformLineItem).toList());
        }

        return dto;
    }

    private InvoiceLineItemDTO transformLineItem(ODataBillingLineItem source) {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemNumber(TransformerUtils.stripLeadingZeros(source.getBillingDocumentItem()));
        dto.setDescription(TransformerUtils.trimSapString(source.getBillingDocumentItemText()));
        dto.setMaterial(TransformerUtils.trimSapString(source.getMaterial()));
        dto.setQuantity(TransformerUtils.parseSapAmount(source.getBillingQuantity()));
        dto.setQuantityUnit(TransformerUtils.trimSapString(source.getBillingQuantityUnit()));
        dto.setNetAmount(TransformerUtils.parseSapAmount(source.getNetAmount()));
        dto.setTaxAmount(TransformerUtils.parseSapAmount(source.getTaxAmount()));
        dto.setCurrency(TransformerUtils.trimSapString(source.getTransactionCurrency()));
        dto.setChargingCategory(TransformerUtils.trimSapString(source.getChargingCategory()));
        return dto;
    }

    /**
     * Derive {@link InvoiceStatus} from SAP clearing status and cancellation flag.
     * <ul>
     *   <li>Cancelled document → REVERSED</li>
     *   <li>ClearingStatus "1" → CLEARED</li>
     *   <li>ClearingStatus "2" → PARTIALLY_PAID</li>
     *   <li>Otherwise: OVERDUE if past due, else OPEN</li>
     * </ul>
     */
    private InvoiceStatus deriveStatus(ODataBillingDocument doc) {
        if (Boolean.TRUE.equals(doc.getBillingDocumentIsCancelled())) {
            return InvoiceStatus.REVERSED;
        }
        String cs = doc.getClearingStatus();
        if ("1".equals(cs)) {
            return InvoiceStatus.CLEARED;
        }
        if ("2".equals(cs)) {
            return InvoiceStatus.PARTIALLY_PAID;
        }
        LocalDate due = TransformerUtils.parseSapDate(doc.getPaymentDueDate());
        if (due != null && due.isBefore(LocalDate.now())) {
            return InvoiceStatus.OVERDUE;
        }
        return InvoiceStatus.OPEN;
    }
}
