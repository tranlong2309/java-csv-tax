package com.company.taxlibrary.exception;

/** Indicates that CSV input does not match the expected structure. */
public class InvalidCsvFormatException extends TaxProcessingException {
    private static final long serialVersionUID = 1L;

    public InvalidCsvFormatException(String message) {
        super(message);
    }

    public InvalidCsvFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCsvFormatException(Throwable cause) {
        super(cause);
    }
}