package com.company.csvengine.exception;
public class CsvColumnMappingConfigException extends CsvProcessingException {
    public CsvColumnMappingConfigException(String message) { super(message); }
    public CsvColumnMappingConfigException(Throwable cause) { super(cause); }
    public CsvColumnMappingConfigException(String message, Throwable cause) { super(message, cause); }
}
