package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw OData response object for a FI-CA contract account.
 * Maps the {@code ContractAccount} entity set from the
 * Contract Account (FI-CA) API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataContractAccount {

    /** VKONT — contract account number (may have leading zeros). */
    @JsonProperty("ContractAccount")
    private String contractAccount;

    @JsonProperty("ContractAccountName")
    private String contractAccountName;

    @JsonProperty("BusinessPartner")
    private String businessPartner;

    @JsonProperty("ContractAccountCategory")
    private String contractAccountCategory;

    /** KOFIZ — account determination ID. */
    @JsonProperty("AccountDeterminationCode")
    private String accountDeterminationCode;

    @JsonProperty("DunningProcedure")
    private String dunningProcedure;

    @JsonProperty("IncomingPaymentMethod")
    private String incomingPaymentMethod;

    @JsonProperty("OutgoingPaymentMethod")
    private String outgoingPaymentMethod;

    @JsonProperty("PaymentCondition")
    private String paymentCondition;

    @JsonProperty("ClearingCategory")
    private String clearingCategory;

    // --- getters / setters ---

    public String getContractAccount() { return contractAccount; }
    public void setContractAccount(String contractAccount) { this.contractAccount = contractAccount; }

    public String getContractAccountName() { return contractAccountName; }
    public void setContractAccountName(String contractAccountName) { this.contractAccountName = contractAccountName; }

    public String getBusinessPartner() { return businessPartner; }
    public void setBusinessPartner(String businessPartner) { this.businessPartner = businessPartner; }

    public String getContractAccountCategory() { return contractAccountCategory; }
    public void setContractAccountCategory(String contractAccountCategory) { this.contractAccountCategory = contractAccountCategory; }

    public String getAccountDeterminationCode() { return accountDeterminationCode; }
    public void setAccountDeterminationCode(String accountDeterminationCode) { this.accountDeterminationCode = accountDeterminationCode; }

    public String getDunningProcedure() { return dunningProcedure; }
    public void setDunningProcedure(String dunningProcedure) { this.dunningProcedure = dunningProcedure; }

    public String getIncomingPaymentMethod() { return incomingPaymentMethod; }
    public void setIncomingPaymentMethod(String incomingPaymentMethod) { this.incomingPaymentMethod = incomingPaymentMethod; }

    public String getOutgoingPaymentMethod() { return outgoingPaymentMethod; }
    public void setOutgoingPaymentMethod(String outgoingPaymentMethod) { this.outgoingPaymentMethod = outgoingPaymentMethod; }

    public String getPaymentCondition() { return paymentCondition; }
    public void setPaymentCondition(String paymentCondition) { this.paymentCondition = paymentCondition; }

    public String getClearingCategory() { return clearingCategory; }
    public void setClearingCategory(String clearingCategory) { this.clearingCategory = clearingCategory; }
}
