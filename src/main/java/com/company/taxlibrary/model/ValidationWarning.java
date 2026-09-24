package com.company.taxlibrary.model;

import org.jspecify.annotations.NonNull;

public record ValidationWarning(
    int lineNumber,
    @NonNull String code,
    @NonNull String message
) {
    public int getLineNumber() { return lineNumber(); }
    public String getCode() { return code(); }
    public String getMessage() { return message(); }
}