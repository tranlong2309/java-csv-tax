package com.company.taxlibrary.parser;

import com.company.taxlibrary.config.MetadataConfig;
import com.company.taxlibrary.exception.InvalidCsvFormatException;
import com.company.taxlibrary.model.TaxItemInput;
import com.company.taxlibrary.model.ValidationWarning;
import com.company.taxlibrary.util.HeaderNormalizer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Streams CSV records into immutable input rows without buffering the file. */
public final class CsvReaderEngine {
    private final MetadataConfig metadataConfig;

    public CsvReaderEngine() {
        this(MetadataConfig.defaults());
    }

    public CsvReaderEngine(MetadataConfig metadataConfig) {
        this.metadataConfig = metadataConfig == null ? MetadataConfig.defaults() : metadataConfig;
    }

    public void read(
            Reader reader,
            Consumer<TaxItemInput> rowConsumer,
            Consumer<ValidationWarning> warningConsumer) {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(rowConsumer, "rowConsumer must not be null");
        Consumer<ValidationWarning> warnings = warningConsumer == null ? warning -> { } : warningConsumer;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter())
                .setIgnoreEmptyLines(true)
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
        try (CSVParser parser = CSVParser.builder()
            .setReader(reader)
            .setFormat(format)
            .get()) {
            List<String> headers = parser.getHeaderNames();
            Map<String, Integer> indexes = HeaderNormalizer.resolveColumnIndexes(headers, metadataConfig);
            validateRequiredHeaders(indexes, headers, warnings);

            for (CSVRecord record : parser) {
                int lineNumber = (int) record.getRecordNumber();
                if (record.size() < headers.size()) {
                    handleInvalidRow(lineNumber, "CSV_ROW_TOO_SHORT",
                            "CSV row contains fewer fields than the header", warnings);
                    continue;
                }
                Map<String, String> values = new java.util.LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    values.put(headers.get(index), getField(record, index));
                }
                rowConsumer.accept(new TaxItemInput(lineNumber, values));
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof InvalidCsvFormatException) {
                throw (InvalidCsvFormatException) exception;
            }
            throw new InvalidCsvFormatException("Unable to read CSV input", exception);
        }
    }

    public void read(Reader reader, Consumer<TaxItemInput> rowConsumer) {
        read(reader, rowConsumer, null);
    }

    public void read(File file, Consumer<TaxItemInput> rowConsumer, Consumer<ValidationWarning> warningConsumer) {
        Objects.requireNonNull(file, "file must not be null");
        try (Reader reader = Files.newBufferedReader(file.toPath(), Charset.forName(metadataConfig.getCharset()))) {
            read(reader, rowConsumer, warningConsumer);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to open CSV file: " + file, exception);
        }
    }

    private void validateRequiredHeaders(
            Map<String, Integer> indexes,
            List<String> headers,
            Consumer<ValidationWarning> warnings) {
        String[] requiredFields = {HeaderNormalizer.ITEM_NAME, HeaderNormalizer.QUANTITY,
                HeaderNormalizer.UNIT_PRICE, HeaderNormalizer.VAT_RATE};
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields) {
            if (!indexes.containsKey(field)) {
                missing.add(field);
            }
        }
        if (!missing.isEmpty()) {
            String message = "Missing required CSV columns: " + String.join(", ", missing);
            handleInvalidRow(1, "MISSING_REQUIRED_HEADER", message, warnings);
        }
    }

    private void handleInvalidRow(
            int lineNumber,
            String code,
            String message,
            Consumer<ValidationWarning> warnings) {
        ValidationWarning warning = new ValidationWarning(lineNumber, code, message);
        if (metadataConfig.isLenientMode()) {
            warnings.accept(warning);
            return;
        }
        throw new InvalidCsvFormatException(message);
    }

    private String getField(CSVRecord record, int index) {
        return record.get(index);
    }

    private char delimiter() {
        String delimiter = metadataConfig.getCsvDelimiter();
        if (delimiter.length() != 1) {
            throw new InvalidCsvFormatException("CSV delimiter must contain exactly one character");
        }
        return delimiter.charAt(0);
    }
}