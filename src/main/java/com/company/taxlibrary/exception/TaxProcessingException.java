package com.company.taxlibrary.exception;

public class TaxProcessingException extends RuntimeException {
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