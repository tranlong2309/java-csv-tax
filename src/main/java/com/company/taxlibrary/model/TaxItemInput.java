package com.company.taxlibrary.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Raw CSV data for one input row. */
public final class TaxItemInput {
    private final int lineNumber;
    private final Map<String, String> rawDataMap;

    public TaxItemInput(int lineNumber, Map<String, String> rawDataMap) {
        this.lineNumber = lineNumber;
        this.rawDataMap = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(rawDataMap, "rawDataMap must not be null")));
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Map<String, String> getRawDataMap() {
        return rawDataMap;
    }
}