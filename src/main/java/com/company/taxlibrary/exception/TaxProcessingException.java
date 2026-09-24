package com.company.taxlibrary.exception;

/** Base unchecked exception for CSV tax processing failures. */
public class TaxProcessingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TaxProcessingException(String message) {
        super(message);
    }

    public TaxProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaxProcessingException(Throwable cause) {
        super(cause);
    }
}