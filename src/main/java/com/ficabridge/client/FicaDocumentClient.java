package com.ficabridge.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficabridge.model.odata.ODataFicaDocument;
import com.ficabridge.model.odata.ODataWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OData client for API_FICADOCUMENT (posted FI-CA accounting documents / open items).
 */
@Component
public class FicaDocumentClient extends ODataClientBase {

    private static final String BASE_PATH = "/API_FICADOCUMENT/FiCADocument";

    public FicaDocumentClient(WebClient sapODataWebClient, ObjectMapper objectMapper) {
        super(sapODataWebClient, objectMapper);
    }

    /**
     * Fetch all FI-CA documents for the given contract account.
     *
     * @param contractAccount VKONT value
     * @return list of FI-CA documents
     */
    public List<ODataFicaDocument> findByContractAccount(String contractAccount) {
        String filter = "ContractAccount eq '" + contractAccount + "'";
        return fetchList(BASE_PATH, filter, null, null,
                new TypeReference<ODataWrapper<ODataFicaDocument>>() {});
    }

    /**
     * Fetch open (uncleared) FI-CA items for the given contract account.
     *
     * @param contractAccount VKONT value
     * @return list of open FI-CA documents
     */
    public List<ODataFicaDocument> findOpenItemsByContractAccount(String contractAccount) {
        String filter = "ContractAccount eq '" + contractAccount + "' and ClearingStatus eq 'OPEN'";
        return fetchList(BASE_PATH, filter, null, null,
                new TypeReference<ODataWrapper<ODataFicaDocument>>() {});
    }

    /**
     * Fetch a single FI-CA document by OPBEL.
     *
     * @param ficaDocument FI-CA document number
     * @return document, or {@code null} if not found
     */
    public ODataFicaDocument findById(String ficaDocument) {
        String path = BASE_PATH + "('" + ficaDocument + "')";
        return fetchSingle(path, null, ODataFicaDocument.class);
    }
}
