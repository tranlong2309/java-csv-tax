package com.company.csvengine.parser;
import com.company.csvengine.util.CsvSanitizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CsvWriterEngine {
    public void write(Iterable<Map<String, String>> rows, Writer writer, List<String> headers, char delimiter) {
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .build())) {
            
            List<String> sanitizedHeaders = new ArrayList<>();
            for (String h : headers) sanitizedHeaders.add(CsvSanitizer.sanitize(h));
            printer.printRecord(sanitizedHeaders);
            
            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>(headers.size());
                for (String header : headers) {
                    values.add(CsvSanitizer.sanitize(row.get(header)));
                }
                printer.printRecord(values);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write CSV output", exception);
        }
    }
}
