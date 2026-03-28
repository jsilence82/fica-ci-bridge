package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw OData response object for a Convergent Invoicing billing document.
 * Maps the {@code BillingDocument} entity set from the
 * Convergent Invoicing – Billing Document API.
 * Field names match the SAP OData metadata exactly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataBillingDocument {

    @JsonProperty("BillingDocument")
    private String billingDocument;

    @JsonProperty("BillingDocumentType")
    private String billingDocumentType;

    @JsonProperty("BillingDocumentDate")
    private String billingDocumentDate;

    @JsonProperty("BillingDocumentIsCancelled")
    private Boolean billingDocumentIsCancelled;

    @JsonProperty("CustomerNumber")
    private String customerNumber;

    @JsonProperty("ContractAccount")
    private String contractAccount;

    @JsonProperty("NetAmount")
    private String netAmount;

    @JsonProperty("TaxAmount")
    private String taxAmount;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    @JsonProperty("PaymentDueDate")
    private String paymentDueDate;

    @JsonProperty("ClearingDate")
    private String clearingDate;

    @JsonProperty("ClearingStatus")
    private String clearingStatus;

    /** Expanded line items — populated when $expand=to_BillingDocumentItem is used. */
    @JsonProperty("to_BillingDocumentItem")
    private ODataWrapper<ODataBillingLineItem> toLineItems;

    // --- getters / setters ---

    public String getBillingDocument() { return billingDocument; }
    public void setBillingDocument(String billingDocument) { this.billingDocument = billingDocument; }

    public String getBillingDocumentType() { return billingDocumentType; }
    public void setBillingDocumentType(String billingDocumentType) { this.billingDocumentType = billingDocumentType; }

    public String getBillingDocumentDate() { return billingDocumentDate; }
    public void setBillingDocumentDate(String billingDocumentDate) { this.billingDocumentDate = billingDocumentDate; }

    public Boolean getBillingDocumentIsCancelled() { return billingDocumentIsCancelled; }
    public void setBillingDocumentIsCancelled(Boolean billingDocumentIsCancelled) { this.billingDocumentIsCancelled = billingDocumentIsCancelled; }

    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

    public String getContractAccount() { return contractAccount; }
    public void setContractAccount(String contractAccount) { this.contractAccount = contractAccount; }

    public String getNetAmount() { return netAmount; }
    public void setNetAmount(String netAmount) { this.netAmount = netAmount; }

    public String getTaxAmount() { return taxAmount; }
    public void setTaxAmount(String taxAmount) { this.taxAmount = taxAmount; }

    public String getTransactionCurrency() { return transactionCurrency; }
    public void setTransactionCurrency(String transactionCurrency) { this.transactionCurrency = transactionCurrency; }

    public String getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(String paymentDueDate) { this.paymentDueDate = paymentDueDate; }

    public String getClearingDate() { return clearingDate; }
    public void setClearingDate(String clearingDate) { this.clearingDate = clearingDate; }

    public String getClearingStatus() { return clearingStatus; }
    public void setClearingStatus(String clearingStatus) { this.clearingStatus = clearingStatus; }

    public List<ODataBillingLineItem> getLineItems() {
        return toLineItems != null ? toLineItems.getResults() : List.of();
    }
    public void setToLineItems(ODataWrapper<ODataBillingLineItem> toLineItems) { this.toLineItems = toLineItems; }
}
