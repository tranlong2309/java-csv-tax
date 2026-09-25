package com.company.csvengine.config;
import com.company.csvengine.exception.MetadataConfigException;
import com.company.csvengine.util.HeaderNormalizer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CsvColumnMappingConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String csvDelimiter;
    private final String charset;
    private final boolean lenientMode;
    private final Map<String, List<String>> columnMapping;

    @JsonCreator
    public CsvColumnMappingConfig(
            @JsonProperty("csvDelimiter") String csvDelimiter,
            @JsonProperty("charset") String charset,
            @JsonProperty("lenientMode") Boolean lenientMode,
            @JsonProperty("columnMapping") Map<String, List<String>> columnMapping) {
        this.csvDelimiter = defaultIfNullOrBlank(csvDelimiter, ",");
        this.charset = defaultIfNullOrBlank(charset, "UTF-8");
        this.lenientMode = lenientMode == null || lenientMode;
        this.columnMapping = columnMapping == null ? Collections.emptyMap() : columnMapping;
    }

    public static CsvColumnMappingConfig fromJson(String json) {
        if (json == null) throw new MetadataConfigException("Metadata JSON must not be null");
        try { return OBJECT_MAPPER.readValue(json, CsvColumnMappingConfig.class); }
        catch (IOException e) { throw new MetadataConfigException("Unable to parse metadata", e); }
    }

    public static CsvColumnMappingConfig fromJson(Reader reader) {
        try { return OBJECT_MAPPER.readValue(reader, CsvColumnMappingConfig.class); }
        catch (IOException e) { throw new MetadataConfigException("Unable to parse metadata", e); }
    }

    public static CsvColumnMappingConfig fromJson(File file) {
        try { return OBJECT_MAPPER.readValue(file, CsvColumnMappingConfig.class); }
        catch (IOException e) { throw new MetadataConfigException("Unable to parse metadata JSON", e); }
    }

    public String getCsvDelimiter() { return csvDelimiter; }
    public String getCharset() { return charset; }
    public boolean isLenientMode() { return lenientMode; }
    public Map<String, List<String>> getColumnMapping() { return columnMapping; }

    public String resolveColumnName(String logicalField, List<String> actualHeaders) {
        List<String> aliases = columnMapping.get(logicalField);
        if (aliases == null || aliases.isEmpty()) aliases = Collections.singletonList(logicalField);
        for (String actualHeader : actualHeaders) {
            String normActual = HeaderNormalizer.normalize(actualHeader);
            for (String alias : aliases) {
                if (normActual.equals(HeaderNormalizer.normalize(alias))) return actualHeader;
            }
        }
        return null;
    }
    private static String defaultIfNullOrBlank(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
