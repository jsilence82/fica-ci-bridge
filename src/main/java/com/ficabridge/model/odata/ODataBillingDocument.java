package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Raw OData V4 response object for a CA Invoicing Document header.
 * Maps the {@code CAInvcgDocument} entity set from API_CAINVOICINGDOCUMENT.
 * Field names match the SAP OData metadata exactly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataBillingDocument {

    @JsonProperty("CAInvoicingDocument")
    private String caInvoicingDocument;

    @JsonProperty("CAInvcgDocumentType")
    private String caInvcgDocumentType;

    @JsonProperty("DocumentDate")
    private LocalDate documentDate;

    @JsonProperty("CAPostingDate")
    private LocalDate caPostingDate;

    /** Net due date — primary due date for payment terms. */
    @JsonProperty("CANetDueDate")
    private LocalDate caNetDueDate;

    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("ContractAccount")
    private String contractAccount;

    @JsonProperty("CAContract")
    private String caContract;

    @JsonProperty("CompanyCode")
    private String companyCode;

    /** Total amount in transaction currency. Signed per FI-CA convention. */
    @JsonProperty("CAAmountInTransactionCurrency")
    private String caAmountInTransactionCurrency;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    /** Official Document Number (ODN) — externally visible reference number. */
    @JsonProperty("CAOfficialDocumentNumber")
    private String caOfficialDocumentNumber;

    /** Non-blank when this document HAS been reversed by another document. */
    @JsonProperty("CAInvcgReversalDocument")
    private String caInvcgReversalDocument;

    /** Non-blank when this document IS itself a reversal of another document. */
    @JsonProperty("CAInvcgReversedDocument")
    private String caInvcgReversedDocument;

    @JsonProperty("CAInvcgIsDocumentPosted")
    private Boolean caInvcgIsDocumentPosted;

    @JsonProperty("CAInvcgIsDocumentPreliminary")
    private Boolean caInvcgIsDocumentPreliminary;

    @JsonProperty("CAInvcgCreationDate")
    private LocalDate caInvcgCreationDate;

    @JsonProperty("CAInvcgDocPeriodStartDate")
    private LocalDate caInvcgDocPeriodStartDate;

    /** Expanded items — populated when {@code $expand=_CAInvcgDocItem} is used. */
    @JsonProperty("_CAInvcgDocItem")
    private ODataWrapper<ODataBillingLineItem> expandedItems;

    // --- getters / setters ---

    public String getCaInvoicingDocument() { return caInvoicingDocument; }
    public void setCaInvoicingDocument(String v) { this.caInvoicingDocument = v; }

    public String getCaInvcgDocumentType() { return caInvcgDocumentType; }
    public void setCaInvcgDocumentType(String v) { this.caInvcgDocumentType = v; }

    public LocalDate getDocumentDate() { return documentDate; }
    public void setDocumentDate(LocalDate v) { this.documentDate = v; }

    public LocalDate getCaPostingDate() { return caPostingDate; }
    public void setCaPostingDate(LocalDate v) { this.caPostingDate = v; }

    public LocalDate getCaNetDueDate() { return caNetDueDate; }
    public void setCaNetDueDate(LocalDate v) { this.caNetDueDate = v; }

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String v) { this.businessPartner = v; }

    public String getContractAccount() { return contractAccount; }
    public void setContractAccount(String v) { this.contractAccount = v; }

    public String getCaContract() { return caContract; }
    public void setCaContract(String v) { this.caContract = v; }

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String v) { this.companyCode = v; }

    public String getCaAmountInTransactionCurrency() { return caAmountInTransactionCurrency; }
    public void setCaAmountInTransactionCurrency(String v) { this.caAmountInTransactionCurrency = v; }

    public String getTransactionCurrency() { return transactionCurrency; }
    public void setTransactionCurrency(String v) { this.transactionCurrency = v; }

    public String getCaOfficialDocumentNumber() { return caOfficialDocumentNumber; }
    public void setCaOfficialDocumentNumber(String v) { this.caOfficialDocumentNumber = v; }

    public String getCaInvcgReversalDocument() { return caInvcgReversalDocument; }
    public void setCaInvcgReversalDocument(String v) { this.caInvcgReversalDocument = v; }

    public String getCaInvcgReversedDocument() { return caInvcgReversedDocument; }
    public void setCaInvcgReversedDocument(String v) { this.caInvcgReversedDocument = v; }

    public Boolean getCaInvcgIsDocumentPosted() { return caInvcgIsDocumentPosted; }
    public void setCaInvcgIsDocumentPosted(Boolean v) { this.caInvcgIsDocumentPosted = v; }

    public Boolean getCaInvcgIsDocumentPreliminary() { return caInvcgIsDocumentPreliminary; }
    public void setCaInvcgIsDocumentPreliminary(Boolean v) { this.caInvcgIsDocumentPreliminary = v; }

    public LocalDate getCaInvcgCreationDate() { return caInvcgCreationDate; }
    public void setCaInvcgCreationDate(LocalDate v) { this.caInvcgCreationDate = v; }

    public LocalDate getCaInvcgDocPeriodStartDate() { return caInvcgDocPeriodStartDate; }
    public void setCaInvcgDocPeriodStartDate(LocalDate v) { this.caInvcgDocPeriodStartDate = v; }

    public List<ODataBillingLineItem> getLineItems() {
        return expandedItems != null ? expandedItems.getResults() : List.of();
    }
    public void setExpandedItems(ODataWrapper<ODataBillingLineItem> v) { this.expandedItems = v; }
}
