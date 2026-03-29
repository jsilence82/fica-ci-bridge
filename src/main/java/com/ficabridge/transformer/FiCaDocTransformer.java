package com.ficabridge.transformer;

import com.ficabridge.model.dto.FicaDocumentDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.odata.ODataFicaDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Transforms a FI-CA document OData response into a {@link FicaDocumentDTO}.
 * Handles SAP clearing-status mapping, OVERDUE derivation, and zero-strip on key IDs.
 */
@Component
public class FiCaDocTransformer {

    /**
     * Map a single OData FI-CA document to a {@link FicaDocumentDTO}.
     */
    public FicaDocumentDTO transform(ODataFicaDocument source) {
        FicaDocumentDTO dto = new FicaDocumentDTO();

        dto.setDocumentNumber(TransformerUtils.stripLeadingZeros(source.getFicaDocument()));
        dto.setItemNumber(TransformerUtils.stripLeadingZeros(source.getFicaDocumentItem()));
        dto.setContractAccount(TransformerUtils.stripLeadingZeros(source.getContractAccount()));
        dto.setBusinessPartner(TransformerUtils.stripLeadingZeros(source.getBusinessPartner()));
        dto.setDocumentDate(TransformerUtils.parseSapDate(source.getDocumentDate()));
        dto.setPostingDate(TransformerUtils.parseSapDate(source.getPostingDate()));
        dto.setDueDate(TransformerUtils.parseSapDate(source.getDueDate()));
        dto.setClearingDate(TransformerUtils.parseSapDate(source.getClearingDate()));
        dto.setAmount(TransformerUtils.parseSapAmount(source.getAmount()));
        dto.setCurrency(TransformerUtils.trimSapString(source.getCurrency()));
        dto.setConditionType(TransformerUtils.trimSapString(source.getConditionType()));
        dto.setDocumentType(TransformerUtils.trimSapString(source.getFicaDocumentType()));
        dto.setStatus(deriveStatus(source));

        return dto;
    }

    /**
     * Derive {@link InvoiceStatus} from SAP AUGST clearing status.
     * <ul>
     *   <li>ClearingStatus "1" → CLEARED</li>
     *   <li>ClearingStatus "2" → PARTIALLY_PAID</li>
     *   <li>Otherwise: OVERDUE if past due date, else OPEN</li>
     * </ul>
     */
    private InvoiceStatus deriveStatus(ODataFicaDocument doc) {
        String cs = doc.getClearingStatus();
        if ("1".equals(cs)) {
            return InvoiceStatus.CLEARED;
        }
        if ("2".equals(cs)) {
            return InvoiceStatus.PARTIALLY_PAID;
        }
        LocalDate due = TransformerUtils.parseSapDate(doc.getDueDate());
        if (due != null && due.isBefore(LocalDate.now())) {
            return InvoiceStatus.OVERDUE;
        }
        return InvoiceStatus.OPEN;
    }
}
