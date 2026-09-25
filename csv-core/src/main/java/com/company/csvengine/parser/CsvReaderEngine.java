package com.company.csvengine.parser;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.exception.InvalidCsvFormatException;
import com.company.csvengine.model.ValidationWarning;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class CsvReaderEngine {
    private final CsvColumnMappingConfig config;

    public CsvReaderEngine(CsvColumnMappingConfig config) {
        this.config = config;
    }

    public void read(
            Reader reader,
            Consumer<Map<String, String>> rowConsumer,
            Consumer<ValidationWarning> warningConsumer) {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(rowConsumer, "rowConsumer must not be null");

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(config.getCsvDelimiter().charAt(0))
                .setIgnoreEmptyLines(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (CSVParser parser = CSVParser.builder()
                .setReader(reader)
                .setFormat(format)
                .get()) {
            
            List<String> headers = parser.getHeaderNames();
            
            for (CSVRecord record : parser) {
                Map<String, String> rawRow = new LinkedHashMap<>();
                if (config.getColumnMapping() != null && !config.getColumnMapping().isEmpty()) {
                    for (String logicalField : config.getColumnMapping().keySet()) {
                        String actualHeader = config.resolveColumnName(logicalField, headers);
                        if (actualHeader != null && record.isMapped(actualHeader)) {
                            rawRow.put(logicalField, record.get(actualHeader));
                        }
                    }
                } else {
                    for (String header : headers) {
                        if (record.isMapped(header)) {
                            rawRow.put(header, record.get(header));
                        }
                    }
                }
                rowConsumer.accept(rawRow);
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new InvalidCsvFormatException("Unable to read CSV input", exception);
        }
    }
}
