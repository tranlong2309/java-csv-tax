package com.company.taxlibrary.parser;

import com.company.taxlibrary.model.TaxItemInput;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.util.CsvSanitizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Streams sanitized CSV records to a writer. */
public final class CsvWriterEngine {
    public void writeEnriched(Iterable<TaxItemOutput> outputs, Writer writer, char delimiter) {
        Objects.requireNonNull(outputs, "outputs must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .get())) {
            printer.printRecord("item_name", "quantity", "unit_price", "vat_rate",
                    "subtotal", "tien_vat", "tong_thanh_toan");
            for (TaxItemOutput output : outputs) {
                printer.printRecord(
                        CsvSanitizer.sanitize(output.getItemName()),
                        output.getQuantity(),
                        output.getUnitPrice(),
                        output.getVatRate(),
                        output.getSubtotal(),
                        output.getVatAmount(),
                        output.getTotalAmount());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write enriched CSV output", exception);
        }
    }

    public void write(Reader reader, Writer writer, char delimiter) {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        CSVFormat inputFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(true)
            .get();
        try (org.apache.commons.csv.CSVParser parser = org.apache.commons.csv.CSVParser.builder()
                 .setReader(reader)
                 .setFormat(inputFormat)
                 .get();
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setDelimiter(delimiter)
                     .get())) {
            for (org.apache.commons.csv.CSVRecord record : parser) {
                List<String> sanitized = new ArrayList<>(record.size());
                for (String value : record) {
                    sanitized.add(CsvSanitizer.sanitize(value));
                }
                printer.printRecord(sanitized);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write CSV output", exception);
        }
    }

    public void write(Iterable<TaxItemInput> rows, Writer writer, List<String> headers, char delimiter) {
        Objects.requireNonNull(rows, "rows must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
            .get())) {
            printer.printRecord(sanitize(headers));
            for (TaxItemInput row : rows) {
                List<String> values = new ArrayList<>(headers.size());
                Map<String, String> rawData = row.getRawDataMap();
                for (String header : headers) {
                    values.add(CsvSanitizer.sanitize(rawData.get(header)));
                }
                printer.printRecord(values);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write CSV output", exception);
        }
    }

    private List<String> sanitize(List<String> values) {
        List<String> sanitized = new ArrayList<>(values.size());
        for (String value : values) {
            sanitized.add(CsvSanitizer.sanitize(value));
        }
        return sanitized;
    }
}