package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw OData response object for a single billing document line item.
 * Maps the {@code BillingDocumentItem} entity set from the
 * Convergent Invoicing – Billing Document API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataBillingLineItem {

    @JsonProperty("BillingDocument")
    private String billingDocument;

    @JsonProperty("BillingDocumentItem")
    private String billingDocumentItem;

    @JsonProperty("BillingDocumentItemText")
    private String billingDocumentItemText;

    @JsonProperty("Material")
    private String material;

    @JsonProperty("BillingQuantity")
    private String billingQuantity;

    @JsonProperty("BillingQuantityUnit")
    private String billingQuantityUnit;

    @JsonProperty("NetAmount")
    private String netAmount;

    @JsonProperty("TaxAmount")
    private String taxAmount;

    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    @JsonProperty("ChargingCategory")
    private String chargingCategory;

    // --- getters / setters ---

    public String getBillingDocument() { return billingDocument; }
    public void setBillingDocument(String billingDocument) { this.billingDocument = billingDocument; }

    public String getBillingDocumentItem() { return billingDocumentItem; }
    public void setBillingDocumentItem(String billingDocumentItem) { this.billingDocumentItem = billingDocumentItem; }

    public String getBillingDocumentItemText() { return billingDocumentItemText; }
    public void setBillingDocumentItemText(String billingDocumentItemText) { this.billingDocumentItemText = billingDocumentItemText; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getBillingQuantity() { return billingQuantity; }
    public void setBillingQuantity(String billingQuantity) { this.billingQuantity = billingQuantity; }

    public String getBillingQuantityUnit() { return billingQuantityUnit; }
    public void setBillingQuantityUnit(String billingQuantityUnit) { this.billingQuantityUnit = billingQuantityUnit; }

    public String getNetAmount() { return netAmount; }
    public void setNetAmount(String netAmount) { this.netAmount = netAmount; }

    public String getTaxAmount() { return taxAmount; }
    public void setTaxAmount(String taxAmount) { this.taxAmount = taxAmount; }

    public String getTransactionCurrency() { return transactionCurrency; }
    public void setTransactionCurrency(String transactionCurrency) { this.transactionCurrency = transactionCurrency; }

    public String getChargingCategory() { return chargingCategory; }
    public void setChargingCategory(String chargingCategory) { this.chargingCategory = chargingCategory; }
}
