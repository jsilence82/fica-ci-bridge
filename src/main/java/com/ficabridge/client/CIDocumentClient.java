package com.ficabridge.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficabridge.model.odata.ODataCIDocument;
import com.ficabridge.model.odata.ODataWrapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OData V4 client for the FI-CA Convergent Invoicing – Invoicing Document API
 * (API_CAINVOICINGDOCUMENT). Entity set: CAInvcgDocument.
 */
@Component
public class CIDocumentClient extends ODataClientBase {

    private static final String BASE_PATH =
            "/sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001/CAInvcgDocument";
    private static final String EXPAND_ITEMS = "_CAInvcgDocItem";

    public CIDocumentClient(WebClient sapODataWebClient, ObjectMapper objectMapper, RateLimiter sapODataRateLimiter) {
        super(sapODataWebClient, objectMapper, sapODataRateLimiter);
    }

    /**
     * Fetch all invoicing documents for the given contract account.
     *
     * @param contractAccount contract account number (zero-padding optional)
     * @return list of invoicing documents with items expanded
     */
    public List<ODataCIDocument> findByContractAccount(String contractAccount) {
        String filter = "ContractAccount eq '" + contractAccount + "'";
        return fetchList(BASE_PATH, filter, null, EXPAND_ITEMS,
                new TypeReference<ODataWrapper<ODataCIDocument>>() {});
    }

    /**
     * Fetch a single invoicing document by its document number, with items expanded.
     *
     * @param caInvoicingDocument the CAInvoicingDocument key value
     * @return invoicing document, or {@code null} if not found
     */
    public ODataCIDocument findById(String caInvoicingDocument) {
        String path = BASE_PATH + "(CAInvoicingDocument='" + caInvoicingDocument + "')";
        return fetchSingle(path, EXPAND_ITEMS, ODataCIDocument.class);
    }
}
