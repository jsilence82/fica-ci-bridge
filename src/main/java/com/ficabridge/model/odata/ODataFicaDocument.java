package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw OData response object for a posted FI-CA accounting document
 * (API_FICADOCUMENT / FiCADocument entity set).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataFicaDocument {

    /** OPBEL — FI-CA document number. */
    @JsonProperty("FiCADocument")
    private String ficaDocument;

    @JsonProperty("FiCADocumentItem")
    private String ficaDocumentItem;

    /** VKONT — contract account number. */
    @JsonProperty("ContractAccount")
    private String contractAccount;

    /** GPART — business partner number. */
    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("DocumentDate")
    private String documentDate;

    @JsonProperty("PostingDate")
    private String postingDate;

    @JsonProperty("DueDate")
    private String dueDate;

    @JsonProperty("ClearingDate")
    private String clearingDate;

    /** AUGST — clearing/payment status. */
    @JsonProperty("ClearingStatus")
    private String clearingStatus;

    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Currency")
    private String currency;

    /** KSCHL — condition type / charge category. */
    @JsonProperty("ConditionType")
    private String conditionType;

    @JsonProperty("FiCADocumentType")
    private String ficaDocumentType;

    // --- getters / setters ---

    public String getFicaDocument() { return ficaDocument; }
    public void setFicaDocument(String ficaDocument) { this.ficaDocument = ficaDocument; }

    public String getFicaDocumentItem() { return ficaDocumentItem; }
    public void setFicaDocumentItem(String ficaDocumentItem) { this.ficaDocumentItem = ficaDocumentItem; }

    public String getContractAccount() { return contractAccount; }
    public void setContractAccount(String contractAccount) { this.contractAccount = contractAccount; }

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String businessPartner) { this.businessPartner = businessPartner; }

    public String getDocumentDate() { return documentDate; }
    public void setDocumentDate(String documentDate) { this.documentDate = documentDate; }

    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getClearingDate() { return clearingDate; }
    public void setClearingDate(String clearingDate) { this.clearingDate = clearingDate; }

    public String getClearingStatus() { return clearingStatus; }
    public void setClearingStatus(String clearingStatus) { this.clearingStatus = clearingStatus; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public String getFicaDocumentType() { return ficaDocumentType; }
    public void setFicaDocumentType(String ficaDocumentType) { this.ficaDocumentType = ficaDocumentType; }
}
