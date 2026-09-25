package com.company.csvengine.exception;

public class InvalidCsvFormatException extends TaxProcessingException {
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