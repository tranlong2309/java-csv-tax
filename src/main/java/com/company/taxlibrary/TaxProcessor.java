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

/**
 * Thread-safe public facade for CSV tax processing.
 * <p>
 * This class provides a high-level API to process tax records from various CSV sources
 * (File, Path, InputStream, String). It safely parses input data, calculates VAT rates,
 * sanitizes output streams, and generates detailed summary reports while maintaining
 * an O(1) memory footprint via streaming engines.
 * </p>
 * 
 * @see com.company.taxlibrary.builder.TaxProcessorBuilder
 */
public final class TaxProcessor {
    
    private final MetadataConfig metadataConfig;

    /**
     * Initializes the processor with the specified configuration.
     *
     * @param metadataConfig The metadata configuration. Must not be null.
     */
    public TaxProcessor(final MetadataConfig metadataConfig) {
        this.metadataConfig = Objects.requireNonNull(metadataConfig, "metadataConfig must not be null");
    }

    /**
     * Creates a new builder for constructing a TaxProcessor.
     *
     * @return A new instance of TaxProcessorBuilder.
     */
    public static TaxProcessorBuilder builder() {
        return new TaxProcessorBuilder();
    }

    /**
     * Processes a CSV file and calculates the tax metrics based on internal configurations.
     * <p>
     * Ensures deterministic closure of streams using try-with-resources.
     * </p>
     *
     * @param csvFile The CSV file to process. Must not be null.
     * @return A compiled {@link TaxSummaryReport} detailing total subtotals, VAT, and row calculations.
     * @throws InvalidCsvFormatException If the file cannot be accessed or violates schema requirements.
     */
    public TaxSummaryReport process(final File csvFile) {
        Objects.requireNonNull(csvFile, "csvFile must not be null");
        
        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), charset())) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to open CSV file: " + csvFile, exception);
        }
    }

    /**
     * Processes a CSV file path and calculates the tax metrics.
     *
     * @param csvPath The path to the CSV file. Must not be null.
     * @return A compiled {@link TaxSummaryReport}.
     */
    public TaxSummaryReport process(final Path csvPath) {
        Objects.requireNonNull(csvPath, "csvPath must not be null");
        
        return process(csvPath.toFile());
    }

    /**
     * Processes a CSV input stream.
     *
     * @param csvInput The input stream. Must not be null.
     * @return A compiled {@link TaxSummaryReport}.
     */
    public TaxSummaryReport process(final InputStream csvInput) {
        Objects.requireNonNull(csvInput, "csvInput must not be null");
        
        try (InputStream input = csvInput;
             Reader reader = new InputStreamReader(input, charset())) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to read CSV input stream", exception);
        }
    }

    /**
     * Processes a raw CSV string content.
     *
     * @param csvContent The CSV string. Must not be null.
     * @return A compiled {@link TaxSummaryReport}.
     */
    public TaxSummaryReport process(final String csvContent) {
        Objects.requireNonNull(csvContent, "csvContent must not be null");
        
        try (Reader reader = new StringReader(csvContent)) {
            return process(reader);
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Unable to read CSV string", exception);
        }
    }

    /**
     * Core method to process a reader stream, streaming through rows to optimize memory (O(1) complexity).
     *
     * @param csvReader The reader stream. Must not be null.
     * @return A compiled {@link TaxSummaryReport}.
     */
    public TaxSummaryReport process(final Reader csvReader) {
        Objects.requireNonNull(csvReader, "csvReader must not be null");
        
        final ProcessingState state = new ProcessingState();
        final long startedAt = System.nanoTime();
        
        // Execute stream parsing securely and parse rows one by one.
        new CsvReaderEngine(metadataConfig).read(
                csvReader,
                input -> processRow(input, state),
                state.warnings::add
        );
        
        final TaxSummaryReport calculated = TaxCalculator.calculateGrandTotals(state.outputs);
        
        return new TaxSummaryReport(
                calculated.getGrandSubtotal(),
                calculated.getGrandTotalVat(),
                calculated.getGrandTotalAmount(),
                calculated.getItemResults(),
                state.warnings,
                elapsedMillis(startedAt)
        );
    }

    /**
     * Reads the provided file, processes the VAT items, and securely serializes the enriched
     * dataset back into a CSV payload text format.
     *
     * @param csvFile The CSV file to process and export.
     * @return An enriched CSV text string including calculated VAT columns.
     */
    public String processToCsv(final File csvFile) {
        return processToCsvReport(csvFile).getCsv();
    }

    /**
     * Processes a CSV path to an enriched CSV string.
     *
     * @param csvPath The file path to process.
     * @return An enriched CSV text string.
     */
    public String processToCsv(final Path csvPath) {
        return processToCsvReport(csvPath).getCsv();
    }

    /**
     * Processes a CSV input stream to an enriched CSV string.
     *
     * @param csvInput The input stream to process.
     * @return An enriched CSV text string.
     */
    public String processToCsv(final InputStream csvInput) {
        return processToCsvReport(csvInput).getCsv();
    }

    /**
     * Processes a CSV string content to an enriched CSV string.
     *
     * @param csvContent The CSV content.
     * @return An enriched CSV text string.
     */
    public String processToCsv(final String csvContent) {
        return processToCsvReport(csvContent).getCsv();
    }

    /**
     * Helper method to process various source types into a CSV report.
     *
     * @param source The input source (File, Path, InputStream, String).
     * @return The generated CsvReport containing the CSV string.
     */
    private CsvReport processToCsvReport(final Object source) {
        final TaxSummaryReport report;
        
        if (source instanceof File) {
            report = process((File) source);
        } else if (source instanceof Path) {
            report = process((Path) source);
        } else if (source instanceof InputStream) {
            report = process((InputStream) source);
        } else if (source instanceof String) {
            report = process((String) source);
        } else {
            throw new IllegalArgumentException("Unsupported source type");
        }
        
        try (StringWriter writer = new StringWriter()) {
            new CsvWriterEngine().writeEnriched(report.getItemResults(), writer, delimiter());
            return new CsvReport(writer.toString());
        } catch (IOException exception) {
            throw new InvalidCsvFormatException("Failed to generate CSV string", exception);
        }
    }

    /**
     * Processes a single CSV row, mapping columns, parsing values, and calculating VAT.
     *
     * @param input The raw input item.
     * @param state The current processing state to append outputs or warnings.
     */
    private void processRow(final TaxItemInput input, final ProcessingState state) {
        final Map<String, String> values = input.getRawDataMap();
        
        try {
            // Map values efficiently using the pre-configured headers
            final String itemName = mappedValue(values, metadataConfig.getColumnMapping().getItemNameHeader());
            
            final BigDecimalValues numbers = new BigDecimalValues(
                    mappedValue(values, metadataConfig.getColumnMapping().getQuantityHeader()),
                    mappedValue(values, metadataConfig.getColumnMapping().getUnitPriceHeader()),
                    mappedValue(values, metadataConfig.getColumnMapping().getVatRateHeader())
            );
            
            final TaxItemOutput output = TaxCalculator.calculateItem(
                    input.getLineNumber(),
                    itemName,
                    numbers.quantity(),
                    numbers.unitPrice(),
                    HeaderNormalizer.parseVatRate(numbers.vatRate())
            );
            
            state.outputs.add(output);
            
        } catch (IllegalArgumentException exception) {
            // Handle row errors via fast-fail exception if not in lenient mode
            if (!metadataConfig.isLenientMode()) {
                throw new InvalidCsvFormatException("Invalid CSV row " + input.getLineNumber(), exception);
            }
            
            state.warnings.add(new ValidationWarning(
                    input.getLineNumber(), 
                    "INVALID_ROW", 
                    exception.getMessage()
            ));
        }
    }

    /**
     * Extracts a mapped value from a CSV row by matching headers against a list of accepted aliases.
     * <p>
     * Performance optimization: Normalizes aliases once to avoid redundant computations inside the loop.
     * </p>
     *
     * @param values  The raw CSV row values map.
     * @param aliases The list of accepted header aliases.
     * @return The extracted value.
     * @throws IllegalArgumentException If the value is missing or empty.
     */
    private String mappedValue(final Map<String, String> values, final List<String> aliases) {
        // Tối ưu hiệu suất: Chuẩn hóa alias trước để giảm thiểu O(N*M) trong vòng lặp lồng nhau
        final List<String> normalizedAliases = new ArrayList<>(aliases.size());
        for (final String alias : aliases) {
            normalizedAliases.add(HeaderNormalizer.normalize(alias));
        }

        for (final Map.Entry<String, String> entry : values.entrySet()) {
            final String normalizedKey = HeaderNormalizer.normalize(entry.getKey());
            
            for (int i = 0; i < normalizedAliases.size(); i++) {
                if (normalizedKey.equals(normalizedAliases.get(i))) {
                    final String value = entry.getValue();
                    
                    // Kiểm tra giá trị hợp lệ, bắt buộc phải có {} và xuống dòng
                    if (value == null || value.trim().isEmpty()) {
                        throw new IllegalArgumentException("Missing value for " + aliases.get(i));
                    }
                    
                    return value.trim();
                }
            }
        }
        
        throw new IllegalArgumentException("Missing CSV column for aliases " + aliases);
    }

    /**
     * Safely retrieves and parses the charset from metadata.
     *
     * @return The parsed Charset.
     */
    private Charset charset() {
        return Charset.forName(metadataConfig.getCharset());
    }

    /**
     * Safely retrieves and validates the CSV delimiter.
     *
     * @return The delimiter character.
     * @throws InvalidCsvFormatException if delimiter is invalid.
     */
    private char delimiter() {
        final String delimiter = metadataConfig.getCsvDelimiter();
        
        if (delimiter == null || delimiter.length() != 1) {
            throw new InvalidCsvFormatException("CSV delimiter must contain exactly one character");
        }
        
        return delimiter.charAt(0);
    }

    /**
     * Calculates the elapsed time in milliseconds.
     *
     * @param startedAt The starting nano time.
     * @return The elapsed time in milliseconds.
     */
    private long elapsedMillis(final long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    /**
     * Inner class to keep track of the processing state to ensure thread safety.
     */
    private static final class ProcessingState {
        private final List<TaxItemOutput> outputs = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();
    }

    /**
     * Inner record-like structure to encapsulate numeric strings before conversion.
     */
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

    /**
     * Inner helper class to encapsulate the CSV string result.
     */
    private static final class CsvReport {
        private final String csv;

        private CsvReport(String csv) {
            this.csv = csv;
        }

        private String getCsv() {
            return csv;
        }
    }
}