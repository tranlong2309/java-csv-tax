package com.company.csvengine.parser;
import com.company.csvengine.util.CsvSanitizer;
import com.company.csvengine.config.CsvColumnMappingConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CsvWriterEngine {
    
    public void write(Iterable<Map<String, String>> rows, Writer writer, char delimiter) {
        java.util.Iterator<Map<String, String>> it = rows.iterator();
        if (!it.hasNext()) return;
        List<String> headers = new ArrayList<>(it.next().keySet());
        write(rows, writer, headers, delimiter);
    }
    public void write(Iterable<Map<String, String>> rows, Writer writer, List<String> headers, char delimiter) {
        doWrite(rows, writer, headers, headers, delimiter);
    }
    
    public void write(Iterable<Map<String, String>> rows, Writer writer, List<String> logicalFields, CsvColumnMappingConfig config, char delimiter) {
        List<String> displayHeaders = new ArrayList<>();
        for (String field : logicalFields) {
            displayHeaders.add(config.getPreferredHeader(field));
        }
        doWrite(rows, writer, displayHeaders, logicalFields, delimiter);
    }
    
    private void doWrite(Iterable<Map<String, String>> rows, Writer writer, List<String> displayHeaders, List<String> keyFields, char delimiter) {
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .build())) {
            
            List<String> sanitizedHeaders = new ArrayList<>();
            for (String h : displayHeaders) sanitizedHeaders.add(CsvSanitizer.sanitize(h));
            printer.printRecord(sanitizedHeaders);
            
            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>(keyFields.size());
                for (String field : keyFields) {
                    values.add(CsvSanitizer.sanitize(row.get(field)));
                }
                printer.printRecord(values);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write CSV output", exception);
        }
    }
}
