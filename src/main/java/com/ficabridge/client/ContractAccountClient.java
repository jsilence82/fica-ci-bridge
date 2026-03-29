package com.ficabridge.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficabridge.model.odata.ODataContractAccount;
import com.ficabridge.model.odata.ODataWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OData client for the Contract Account (FI-CA) API.
 */
@Component
public class ContractAccountClient extends ODataClientBase {

    private static final String BASE_PATH = "/API_CA_CONTRACTACCOUNT/ContractAccount";

    public ContractAccountClient(WebClient sapODataWebClient, ObjectMapper objectMapper) {
        super(sapODataWebClient, objectMapper);
    }

    /**
     * Fetch a single contract account by VKONT.
     *
     * @param contractAccount contract account number (with or without leading zeros)
     * @return contract account entity, or {@code null} if not found
     */
    public ODataContractAccount findById(String contractAccount) {
        String path = BASE_PATH + "('" + contractAccount + "')";
        return fetchSingle(path, null, ODataContractAccount.class);
    }

    /**
     * Fetch all contract accounts for a given business partner.
     *
     * @param businessPartner GPART value
     * @return list of matching contract accounts
     */
    public java.util.List<ODataContractAccount> findByBusinessPartner(String businessPartner) {
        String filter = "BusinessPartner eq '" + businessPartner + "'";
        return fetchList(BASE_PATH, filter, null, null,
                new TypeReference<ODataWrapper<ODataContractAccount>>() {});
    }
}
