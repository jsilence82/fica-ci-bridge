package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Raw OData V4 response object for a CA Invoicing Document item.
 * Maps the {@code CAInvcgDocItem} entity set from API_CAINVOICINGDOCUMENT.
 * Keyed by CAInvoicingDocument + CAInvcgDocItem (sequential item number).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataCILineItem {

    @JsonProperty("CAInvoicingDocument")
    private String caInvoicingDocument;

    /** Sequential item number within the invoicing document (max 8 chars). */
    @JsonProperty("CAInvcgDocItem")
    private String caInvcgDocItem;

    /** Main transaction code — primary categorisation of the charge. */
    @JsonProperty("CAMainTransaction")
    private String caMainTransaction;

    /** Sub-transaction code — secondary categorisation of the charge. */
    @JsonProperty("CASubTransaction")
    private String caSubTransaction;

    @JsonProperty("CAInvcgDocItemIsReversal")
    private Boolean caInvcgDocItemIsReversal;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    /** Item amount in transaction currency. Signed per FI-CA convention. */
    @JsonProperty("CAAmountInTransactionCurrency")
    private String caAmountInTransactionCurrency;

    /** True when the amount already includes tax (gross pricing). */
    @JsonProperty("CATaxIsIncluded")
    private Boolean caTaxIsIncluded;

    @JsonProperty("TaxCode")
    private String taxCode;

    /** Condition type — pricing condition code (closest equivalent to charging category). */
    @JsonProperty("CAConditionType")
    private String caConditionType;

    @JsonProperty("CATaxAmountInTransCurrency")
    private String caTaxAmountInTransCurrency;

    @JsonProperty("CATaxBaseAmount")
    private String caTaxBaseAmount;

    @JsonProperty("CATaxRateInPercent")
    private String caTaxRateInPercent;

    @JsonProperty("Quantity")
    private String quantity;

    @JsonProperty("UnitOfMeasure")
    private String unitOfMeasure;

    @JsonProperty("UnitOfMeasureISOCode")
    private String unitOfMeasureISOCode;

    @JsonProperty("CANetDueDate")
    private LocalDate caNetDueDate;

    @JsonProperty("CAItemPeriodStartDate")
    private LocalDate caItemPeriodStartDate;

    @JsonProperty("CAItemPeriodEndDate")
    private LocalDate caItemPeriodEndDate;

    /** FI-CA accounting document number linked to this item. */
    @JsonProperty("CADocumentNumber")
    private String caDocumentNumber;

    /** Non-blank when this item has been cleared by a FI-CA payment/clearing doc. */
    @JsonProperty("CAClearingDocumentNumber")
    private String caClearingDocumentNumber;

    @JsonProperty("CAClearingAmountInTransCrcy")
    private String caClearingAmountInTransCrcy;

    // --- getters / setters ---

    public String getCaInvoicingDocument() { return caInvoicingDocument; }
    public void setCaInvoicingDocument(String v) { this.caInvoicingDocument = v; }

    public String getCaInvcgDocItem() { return caInvcgDocItem; }
    public void setCaInvcgDocItem(String v) { this.caInvcgDocItem = v; }

    public String getCaMainTransaction() { return caMainTransaction; }
    public void setCaMainTransaction(String v) { this.caMainTransaction = v; }

    public String getCaSubTransaction() { return caSubTransaction; }
    public void setCaSubTransaction(String v) { this.caSubTransaction = v; }

    public Boolean getCaInvcgDocItemIsReversal() { return caInvcgDocItemIsReversal; }
    public void setCaInvcgDocItemIsReversal(Boolean v) { this.caInvcgDocItemIsReversal = v; }

    public String getTransactionCurrency() { return transactionCurrency; }
    public void setTransactionCurrency(String v) { this.transactionCurrency = v; }

    public String getCaAmountInTransactionCurrency() { return caAmountInTransactionCurrency; }
    public void setCaAmountInTransactionCurrency(String v) { this.caAmountInTransactionCurrency = v; }

    public Boolean getCaTaxIsIncluded() { return caTaxIsIncluded; }
    public void setCaTaxIsIncluded(Boolean v) { this.caTaxIsIncluded = v; }

    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String v) { this.taxCode = v; }

    public String getCaConditionType() { return caConditionType; }
    public void setCaConditionType(String v) { this.caConditionType = v; }

    public String getCaTaxAmountInTransCurrency() { return caTaxAmountInTransCurrency; }
    public void setCaTaxAmountInTransCurrency(String v) { this.caTaxAmountInTransCurrency = v; }

    public String getCaTaxBaseAmount() { return caTaxBaseAmount; }
    public void setCaTaxBaseAmount(String v) { this.caTaxBaseAmount = v; }

    public String getCaTaxRateInPercent() { return caTaxRateInPercent; }
    public void setCaTaxRateInPercent(String v) { this.caTaxRateInPercent = v; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String v) { this.quantity = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public String getUnitOfMeasureISOCode() { return unitOfMeasureISOCode; }
    public void setUnitOfMeasureISOCode(String v) { this.unitOfMeasureISOCode = v; }

    public LocalDate getCaNetDueDate() { return caNetDueDate; }
    public void setCaNetDueDate(LocalDate v) { this.caNetDueDate = v; }

    public LocalDate getCaItemPeriodStartDate() { return caItemPeriodStartDate; }
    public void setCaItemPeriodStartDate(LocalDate v) { this.caItemPeriodStartDate = v; }

    public LocalDate getCaItemPeriodEndDate() { return caItemPeriodEndDate; }
    public void setCaItemPeriodEndDate(LocalDate v) { this.caItemPeriodEndDate = v; }

    public String getCaDocumentNumber() { return caDocumentNumber; }
    public void setCaDocumentNumber(String v) { this.caDocumentNumber = v; }

    public String getCaClearingDocumentNumber() { return caClearingDocumentNumber; }
    public void setCaClearingDocumentNumber(String v) { this.caClearingDocumentNumber = v; }

    public String getCaClearingAmountInTransCrcy() { return caClearingAmountInTransCrcy; }
    public void setCaClearingAmountInTransCrcy(String v) { this.caClearingAmountInTransCrcy = v; }
}
