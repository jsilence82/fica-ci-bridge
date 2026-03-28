package com.ficabridge.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficabridge.model.odata.ODataBillingDocument;
import com.ficabridge.model.odata.ODataWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OData client for the Convergent Invoicing – Billing Document API.
 */
@Component
public class BillingDocumentClient extends ODataClientBase {

    private static final String BASE_PATH = "/API_BILLING_DOCUMENT_SRV/BillingDocument";
    private static final String EXPAND_ITEMS = "to_BillingDocumentItem";

    public BillingDocumentClient(WebClient sapODataWebClient, ObjectMapper objectMapper) {
        super(sapODataWebClient, objectMapper);
    }

    /**
     * Fetch all billing documents for the given contract account number.
     *
     * @param contractAccount VKONT value (leading zeros stripped by caller if needed)
     * @return list of billing documents with line items expanded
     */
    public List<ODataBillingDocument> findByContractAccount(String contractAccount) {
        String filter = "ContractAccount eq '" + contractAccount + "'";
        return fetchList(BASE_PATH, filter, null, EXPAND_ITEMS,
                new TypeReference<ODataWrapper<ODataBillingDocument>>() {});
    }

    /**
     * Fetch a single billing document by its document number, with line items expanded.
     *
     * @param billingDocument billing document number
     * @return billing document, or {@code null} if not found
     */
    public ODataBillingDocument findById(String billingDocument) {
        String path = BASE_PATH + "('" + billingDocument + "')";
        return fetchSingle(path, EXPAND_ITEMS, ODataBillingDocument.class);
    }
}
