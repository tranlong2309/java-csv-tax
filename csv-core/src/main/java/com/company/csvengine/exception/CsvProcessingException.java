package com.company.csvengine.exception;
public class CsvProcessingException extends RuntimeException {
    public CsvProcessingException(String message) { super(message); }
    public CsvProcessingException(String message, Throwable cause) { super(message, cause); }
    public CsvProcessingException(Throwable cause) { super(cause); }
}
