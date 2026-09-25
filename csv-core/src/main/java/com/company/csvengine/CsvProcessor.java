package com.company.csvengine;
import com.company.csvengine.calc.RecordCalculator;
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
    private final RecordCalculator<T> calculator;

    private CsvProcessor(CsvColumnMappingConfig config, RecordCalculator<T> calculator) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public List<T> process(File csvFile) {
        try (Reader reader = Files.newBufferedReader(csvFile.toPath(), Charset.forName(config.getCharset()))) {
            return process(reader);
        } catch (IOException e) {
            throw new InvalidCsvFormatException("Unable to open file", e);
        }
    }

    public List<T> process(InputStream csvInput) {
        try (Reader reader = new InputStreamReader(csvInput, Charset.forName(config.getCharset()))) {
            return process(reader);
        } catch (IOException e) {
            throw new InvalidCsvFormatException("Unable to read stream", e);
        }
    }

    public List<T> process(Reader reader) {
        List<T> results = new ArrayList<>();
        new CsvReaderEngine(config).read(
            reader,
            rawRow -> results.add(calculator.calculate(rawRow)),
            warning -> {}
        );
        return results;
    }

    public static class Builder<T> {
        private CsvColumnMappingConfig config;
        private RecordCalculator<T> calculator;

        public Builder<T> withMetadata(CsvColumnMappingConfig config) {
            this.config = config;
            return this;
        }
        public Builder<T> withCalculator(RecordCalculator<T> calculator) {
            this.calculator = calculator;
            return this;
        }
        public CsvProcessor<T> build() {
            return new CsvProcessor<>(config, calculator);
        }
    }
}
