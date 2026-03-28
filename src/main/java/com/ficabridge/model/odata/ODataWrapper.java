package com.ficabridge.model.odata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic wrapper for SAP OData responses.
 * <p>
 * OData V2 wraps results in {@code { "d": { "results": [] } }}.
 * OData V4 uses {@code { "value": [] }}.
 * This class handles both envelopes so callers need not care which version
 * the specific API returns.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ODataWrapper<T> {

    /** OData V4 top-level value array. */
    @JsonProperty("value")
    private List<T> value;

    /** OData V2 "d" envelope. */
    @JsonProperty("d")
    private V2Envelope<T> d;

    /**
     * Returns the result list regardless of which OData version envelope was used.
     */
    public List<T> getResults() {
        if (value != null) {
            return value;
        }
        if (d != null && d.getResults() != null) {
            return d.getResults();
        }
        return List.of();
    }

    public void setValue(List<T> value) {
        this.value = value;
    }

    public void setD(V2Envelope<T> d) {
        this.d = d;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class V2Envelope<T> {

        @JsonProperty("results")
        private List<T> results;

        public List<T> getResults() {
            return results;
        }

        public void setResults(List<T> results) {
            this.results = results;
        }
    }
}
