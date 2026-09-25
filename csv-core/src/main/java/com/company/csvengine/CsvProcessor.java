package com.company.csvengine;
import com.company.csvengine.calc.BatchCalculator;
import com.company.csvengine.calc.RowCalculator;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.exception.InvalidCsvFormatException;
import com.company.csvengine.parser.CsvReaderEngine;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CsvProcessor<T> {
    private final CsvColumnMappingConfig config;
    private final RowCalculator<T> rowCalculator;
    private final BatchCalculator<T> batchCalculator;

    private CsvProcessor(CsvColumnMappingConfig config, RowCalculator<T> rowCalculator, BatchCalculator<T> batchCalculator) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.rowCalculator = rowCalculator;
        this.batchCalculator = batchCalculator;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    
    public List<T> process(File csvFile) { return process(csvFile, w -> {}); }
    public List<T> process(File csvFile, java.util.function.Consumer<com.company.csvengine.model.ValidationWarning> warnings) {

        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), Charset.forName(config.getCharset()))) {
            return process(reader, warnings);
        } catch (IOException e) {
            throw new InvalidCsvFormatException("Unable to open file", e);
        }
    }

    
    public List<T> process(InputStream csvInput) { return process(csvInput, w -> {}); }
    public List<T> process(InputStream csvInput, java.util.function.Consumer<com.company.csvengine.model.ValidationWarning> warnings) {

        try (Reader reader = new InputStreamReader(csvInput, Charset.forName(config.getCharset()))) {
            return process(reader, warnings);
        } catch (IOException e) {
            throw new InvalidCsvFormatException("Unable to read stream", e);
        }
    }

    
    public List<T> process(Reader reader) { return process(reader, w -> {}); }
    public List<T> process(Reader reader, java.util.function.Consumer<com.company.csvengine.model.ValidationWarning> warnings) {
        if (batchCalculator != null) {
            List<java.util.Map<String, String>> allRows = new ArrayList<>();
            new CsvReaderEngine(config).read(reader, allRows::add, warnings);
            return batchCalculator.calculate(allRows);
        } else {
            List<T> results = new ArrayList<>();
            new CsvReaderEngine(config).read(reader, rawRow -> results.add(rowCalculator.calculate(rawRow)), warnings);
            return results;
        }
    }

    public static class Builder<T> {
        private CsvColumnMappingConfig config;
        private RowCalculator<T> rowCalculator;
        private BatchCalculator<T> batchCalculator;

        public Builder<T> withMetadata(CsvColumnMappingConfig config) {
            this.config = config;
            return this;
        }
        public Builder<T> withRowCalculator(RowCalculator<T> calculator) {
            this.rowCalculator = calculator;
            return this;
        }
        public Builder<T> withBatchCalculator(BatchCalculator<T> calculator) {
            this.batchCalculator = calculator;
            return this;
        }
        public CsvProcessor<T> build() {
            if ((rowCalculator == null && batchCalculator == null) || (rowCalculator != null && batchCalculator != null)) {
                throw new IllegalStateException("Exactly one of RowCalculator or BatchCalculator must be provided");
            }
            return new CsvProcessor<>(config, rowCalculator, batchCalculator);
        }
    }
}
