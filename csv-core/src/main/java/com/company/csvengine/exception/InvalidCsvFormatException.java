package com.company.csvengine.exception;
public class InvalidCsvFormatException extends CsvProcessingException {
    public InvalidCsvFormatException(String message) { super(message); }
    public InvalidCsvFormatException(Throwable cause) { super(cause); }
    public InvalidCsvFormatException(String message, Throwable cause) { super(message, cause); }
}
