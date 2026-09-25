package com.company.csvengine.exception;

/** Indicates invalid or unreadable metadata configuration. */
public class CsvColumnMappingConfigException extends TaxProcessingException {
    private static final long serialVersionUID = 1L;

    public CsvColumnMappingConfigException(String message) {
        super(message);
    }

    public CsvColumnMappingConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}