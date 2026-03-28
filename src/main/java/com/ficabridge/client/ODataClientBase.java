package com.ficabridge.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.odata.ODataWrapper;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for all SAP OData V4 HTTP clients.
 * <p>
 * Handles:
 * <ul>
 *   <li>$filter / $select / $expand query parameter construction</li>
 *   <li>4xx / 5xx error wrapping into {@link ODataClientException}</li>
 *   <li>Response deserialization via {@link ODataWrapper}</li>
 * </ul>
 *
 * Concrete subclasses must supply the entity-set path and response type.
 * Services must never call this class directly — they call the typed methods
 * on concrete client subclasses.
 */
public abstract class ODataClientBase {

    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected ODataClientBase(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetch a list of entities from the given OData entity-set path,
     * applying optional $filter, $select, and $expand parameters.
     *
     * @param entitySetPath path relative to the base URL, e.g. {@code /API_BILLING_DOCUMENT_SRV/BillingDocument}
     * @param filter        OData $filter expression, or {@code null}
     * @param select        comma-separated $select fields, or {@code null}
     * @param expand        comma-separated $expand navigation properties, or {@code null}
     * @param typeRef       Jackson type reference for the wrapper generic type
     * @param <T>           entity type
     * @return list of deserialized entities (never null, may be empty)
     */
    protected <T> List<T> fetchList(String entitySetPath,
                                    String filter,
                                    String select,
                                    String expand,
                                    TypeReference<ODataWrapper<T>> typeRef) {
        try {
            Function<UriBuilder, URI> uriSpec = uriBuilder -> {
                uriBuilder.path(entitySetPath);
                if (filter != null) uriBuilder.queryParam("$filter", filter);
                if (select != null) uriBuilder.queryParam("$select", select);
                if (expand != null) uriBuilder.queryParam("$expand", expand);
                return uriBuilder.build();
            };

            String json = webClient.get()
                    .uri(uriSpec)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isBlank()) {
                return List.of();
            }

            ODataWrapper<T> wrapper = objectMapper.readValue(json, typeRef);
            return wrapper.getResults();

        } catch (WebClientResponseException ex) {
            throw new ODataClientException(
                    "OData request failed [" + entitySetPath + "]: HTTP " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new ODataClientException("OData request failed [" + entitySetPath + "]: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetch a single entity by key.
     *
     * @param entityPath path including the key predicate, e.g. {@code /API_BILLING_DOCUMENT_SRV/BillingDocument('0090001234')}
     * @param expand     comma-separated $expand navigation properties, or {@code null}
     * @param type       target type class
     * @param <T>        entity type
     * @return deserialized entity
     */
    protected <T> T fetchSingle(String entityPath, String expand, Class<T> type) {
        try {
            Function<UriBuilder, URI> uriSpec = uriBuilder -> {
                uriBuilder.path(entityPath);
                if (expand != null) uriBuilder.queryParam("$expand", expand);
                return uriBuilder.build();
            };

            String json = webClient.get()
                    .uri(uriSpec)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isBlank()) {
                return null;
            }

            // V4 single-entity response: the root object IS the entity (no "value" wrapper)
            // V2 single-entity response: wrapped in { "d": { ... } }
            Map<?, ?> raw = objectMapper.readValue(json, Map.class);
            if (raw.containsKey("d")) {
                return objectMapper.convertValue(raw.get("d"), type);
            }
            return objectMapper.readValue(json, type);

        } catch (WebClientResponseException ex) {
            throw new ODataClientException(
                    "OData request failed [" + entityPath + "]: HTTP " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value());
        } catch (Exception ex) {
            throw new ODataClientException("OData request failed [" + entityPath + "]: " + ex.getMessage(), ex);
        }
    }
}
