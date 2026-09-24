package com.company.taxlibrary.model;

import org.jspecify.annotations.NonNull;
import java.util.Map;

public record TaxItemInput(
    int lineNumber,
    @NonNull Map<String, String> rawDataMap
) {
    public TaxItemInput {
        if (rawDataMap == null) {
            rawDataMap = java.util.Collections.emptyMap();
        } else {
            rawDataMap = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(rawDataMap));
        }
    }

    public int getLineNumber() {
        return lineNumber();
    }

    public Map<String, String> getRawDataMap() {
        return rawDataMap();
    }
}