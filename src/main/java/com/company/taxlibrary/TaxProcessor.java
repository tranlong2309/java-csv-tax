package com.company.taxlibrary;

import com.company.taxlibrary.builder.TaxProcessorBuilder;
import com.company.taxlibrary.config.MetadataConfig;
import com.company.taxlibrary.exception.InvalidCsvFormatException;
import com.company.taxlibrary.model.TaxItemInput;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import com.company.taxlibrary.model.ValidationWarning;
import com.company.taxlibrary.parser.CsvReaderEngine;
import com.company.taxlibrary.parser.CsvWriterEngine;
import com.company.taxlibrary.service.TaxCalculator;
import com.company.taxlibrary.util.HeaderNormalizer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Thread-safe public facade for CSV tax processing. */
public final class TaxProcessor {
    private final MetadataConfig metadataConfig;

    public TaxProcessor(MetadataConfig metadataConfig) {
        this.metadataConfig = Objects.requireNonNull(metadataConfig, "metadataConfig must not be null");
    }

    public static TaxProcessorBuilder builder() {
        return new TaxProcessorBuilder();
    }

    public TaxSummaryReport process(File csvFile) {
        Objects.requireNonNull(csvFile, "csvFile must not be null");
        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), charset())) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to open CSV file: " + csvFile, exception);
        }
    }

    public TaxSummaryReport process(Path csvPath) {
        Objects.requireNonNull(csvPath, "csvPath must not be null");
        return process(csvPath.toFile());
    }

    public TaxSummaryReport process(InputStream csvInput) {
        Objects.requireNonNull(csvInput, "csvInput must not be null");
        try (InputStream input = csvInput;
             Reader reader = new InputStreamReader(input, charset())) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to read CSV input stream", exception);
        }
    }

    public TaxSummaryReport process(String csvContent) {
        Objects.requireNonNull(csvContent, "csvContent must not be null");
        try (Reader reader = new StringReader(csvContent)) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to read CSV string", exception);
        }
    }

    public TaxSummaryReport process(Reader csvReader) {
        Objects.requireNonNull(csvReader, "csvReader must not be null");
        ProcessingState state = new ProcessingState();
        long startedAt = System.nanoTime();
        new CsvReaderEngine(metadataConfig).read(
                csvReader,
                input -> processRow(input, state),
                state.warnings::add);
        TaxSummaryReport calculated = TaxCalculator.calculateGrandTotals(state.outputs);
        return new TaxSummaryReport(
                calculated.getGrandSubtotal(),
                calculated.getGrandTotalVat(),
                calculated.getGrandTotalAmount(),
                calculated.getItemResults(),
                state.warnings,
                elapsedMillis(startedAt));
    }

    public String processToCsv(File csvFile) {
        return processToCsvReport(csvFile).getCsv();
    }

    public String processToCsv(Path csvPath) {
        return processToCsvReport(csvPath).getCsv();
    }

    public String processToCsv(InputStream csvInput) {
        return processToCsvReport(csvInput).getCsv();
    }

    public String processToCsv(String csvContent) {
        return processToCsvReport(csvContent).getCsv();
    }

    private CsvReport processToCsvReport(Object source) {
        TaxSummaryReport report;
        if (source instanceof File) {
            report = process((File) source);
        } else if (source instanceof Path) {
            report = process((Path) source);
        } else if (source instanceof InputStream) {
            report = process((InputStream) source);
        } else {
            report = process((String) source);
        }
        StringWriter writer = new StringWriter();
        new CsvWriterEngine().writeEnriched(report.getItemResults(), writer, delimiter());
        return new CsvReport(report, writer.toString());
    }

    private void processRow(TaxItemInput input, ProcessingState state) {
        Map<String, String> values = input.getRawDataMap();
        try {
            String itemName = mappedValue(values, metadataConfig.getColumnMapping().getItemNameHeader());
            BigDecimalValues numbers = new BigDecimalValues(
                    mappedValue(values, metadataConfig.getColumnMapping().getQuantityHeader()),
                    mappedValue(values, metadataConfig.getColumnMapping().getUnitPriceHeader()),
                    mappedValue(values, metadataConfig.getColumnMapping().getVatRateHeader()));
            state.outputs.add(TaxCalculator.calculateItem(
                    input.getLineNumber(),
                    itemName,
                    numbers.quantity(),
                    numbers.unitPrice(),
                    HeaderNormalizer.parseVatRate(numbers.vatRate())));
        } catch (IllegalArgumentException exception) {
            if (!metadataConfig.isLenientMode()) {
                throw new InvalidCsvFormatException("Invalid CSV row " + input.getLineNumber(), exception);
            }
            state.warnings.add(new ValidationWarning(
                    input.getLineNumber(), "INVALID_ROW", exception.getMessage()));
        }
    }

    private String mappedValue(Map<String, String> values, List<String> aliases) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            for (String alias : aliases) {
                if (HeaderNormalizer.normalize(entry.getKey()).equals(HeaderNormalizer.normalize(alias))) {
                    if (entry.getValue() == null || entry.getValue().trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing value for " + alias);
                    }
                    return entry.getValue().trim();
                }
            }
        }
        throw new IllegalArgumentException("Missing CSV column for aliases " + aliases);
    }

    private Charset charset() {
        return Charset.forName(metadataConfig.getCharset());
    }

    private char delimiter() {
        String delimiter = metadataConfig.getCsvDelimiter();
        if (delimiter.length() != 1) {
            throw new InvalidCsvFormatException("CSV delimiter must contain exactly one character");
        }
        return delimiter.charAt(0);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private static final class ProcessingState {
        private final List<TaxItemOutput> outputs = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();
    }

    private static final class BigDecimalValues {
        private final String quantity;
        private final String unitPrice;
        private final String vatRate;

        private BigDecimalValues(String quantity, String unitPrice, String vatRate) {
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.vatRate = vatRate;
        }

        private BigDecimal quantity() {
            return new BigDecimal(quantity);
        }

        private BigDecimal unitPrice() {
            return new BigDecimal(unitPrice);
        }

        private String vatRate() {
            return vatRate;
        }
    }

    private static final class CsvReport {
        private final String csv;

        private CsvReport(TaxSummaryReport report, String csv) {
            this.csv = csv;
        }

        private String getCsv() {
            return csv;
        }
    }
}