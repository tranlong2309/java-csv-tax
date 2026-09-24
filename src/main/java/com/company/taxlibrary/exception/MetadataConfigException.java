package com.company.taxlibrary.exception;

/** Indicates invalid or unreadable metadata configuration. */
public class MetadataConfigException extends TaxProcessingException {
    private static final long serialVersionUID = 1L;

    public MetadataConfigException(String message) {
        super(message);
    }

    public MetadataConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}