package com.company.taxlibrary.model;

import java.util.Objects;

/** A non-fatal validation issue associated with an input row. */
public final class ValidationWarning {
    private final int lineNumber;
    private final String code;
    private final String message;

    public ValidationWarning(int lineNumber, String code, String message) {
        this.lineNumber = lineNumber;
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}