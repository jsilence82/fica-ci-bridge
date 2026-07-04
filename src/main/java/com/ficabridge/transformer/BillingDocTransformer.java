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
 * Transforms a CAInvcgDocument OData V4 response (API_CAINVOICINGDOCUMENT)
 * into the application's {@link InvoiceDTO} domain model.
 *
 * Status derivation logic:
 * <ul>
 *   <li>CAInvcgReversalDocument non-blank → REVERSED (document has been reversed)</li>
 *   <li>All items have CAClearingDocumentNumber → CLEARED</li>
 *   <li>Some items have CAClearingDocumentNumber → PARTIALLY_PAID</li>
 *   <li>No items cleared, CANetDueDate in past → OVERDUE</li>
 *   <li>Otherwise → OPEN</li>
 * </ul>
 */
@Component
public class BillingDocTransformer {

    /**
     * Map a single OData invoicing document to an {@link InvoiceDTO}.
     * Line items are included when the OData response was fetched with
     * {@code $expand=_CAInvcgDocItem}.
     */
    public InvoiceDTO transform(ODataBillingDocument source) {
        InvoiceDTO dto = new InvoiceDTO();

        dto.setBillingDocNumber(TransformerUtils.stripLeadingZeros(source.getCaInvoicingDocument()));
        dto.setContractAccount(TransformerUtils.stripLeadingZeros(source.getContractAccount()));
        dto.setBusinessPartner(TransformerUtils.stripLeadingZeros(source.getBusinessPartner()));
        dto.setDueDate(source.getCaNetDueDate());
        dto.setAmount(TransformerUtils.parseSapAmount(source.getCaAmountInTransactionCurrency()));
        dto.setCurrency(TransformerUtils.trimSapString(source.getTransactionCurrency()));
        dto.setOfficialDocumentNumber(TransformerUtils.trimSapString(source.getCaOfficialDocumentNumber()));
        dto.setStatus(deriveStatus(source));

        List<ODataBillingLineItem> rawItems = source.getLineItems();
        if (rawItems != null && !rawItems.isEmpty()) {
            dto.setLineItems(rawItems.stream().map(this::transformLineItem).toList());
        }

        return dto;
    }

    private InvoiceLineItemDTO transformLineItem(ODataBillingLineItem source) {
        InvoiceLineItemDTO dto = new InvoiceLineItemDTO();
        dto.setItemNumber(TransformerUtils.stripLeadingZeros(source.getCaInvcgDocItem()));
        dto.setQuantity(TransformerUtils.parseSapAmount(source.getQuantity()));
        dto.setQuantityUnit(TransformerUtils.trimSapString(source.getUnitOfMeasure()));
        dto.setNetAmount(TransformerUtils.parseSapAmount(source.getCaAmountInTransactionCurrency()));
        dto.setTaxAmount(TransformerUtils.parseSapAmount(source.getCaTaxAmountInTransCurrency()));
        dto.setCurrency(TransformerUtils.trimSapString(source.getTransactionCurrency()));
        dto.setChargingCategory(TransformerUtils.trimSapString(source.getCaConditionType()));
        // description and material have no equivalent in API_CAINVOICINGDOCUMENT
        return dto;
    }

    private InvoiceStatus deriveStatus(ODataBillingDocument doc) {
        // Reversed: a reversal document exists for this invoicing document
        String reversalDoc = doc.getCaInvcgReversalDocument();
        if (reversalDoc != null && !reversalDoc.isBlank()) {
            return InvoiceStatus.REVERSED;
        }

        // Clearing status derived from item-level CAClearingDocumentNumber
        List<ODataBillingLineItem> items = doc.getLineItems();
        if (!items.isEmpty()) {
            long clearedCount = items.stream()
                    .filter(i -> i.getCaClearingDocumentNumber() != null
                            && !i.getCaClearingDocumentNumber().isBlank())
                    .count();
            if (clearedCount == items.size()) {
                return InvoiceStatus.CLEARED;
            }
            if (clearedCount > 0) {
                return InvoiceStatus.PARTIALLY_PAID;
            }
        }

        LocalDate due = doc.getCaNetDueDate();
        if (due != null && due.isBefore(LocalDate.now())) {
            return InvoiceStatus.OVERDUE;
        }
        return InvoiceStatus.OPEN;
    }
}
