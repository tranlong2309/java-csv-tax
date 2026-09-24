package com.company.taxlibrary.builder;

import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.config.MetadataConfig;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Objects;

/** Fluent, single-use configuration builder for {@link TaxProcessor}. */
public final class TaxProcessorBuilder {
    private MetadataConfig metadataConfig = MetadataConfig.defaults();

    public synchronized TaxProcessorBuilder withMetadata(File metadataFile) {
        metadataConfig = MetadataConfig.fromJson(metadataFile);
        return this;
    }

    public synchronized TaxProcessorBuilder withMetadata(Path metadataPath) {
        metadataConfig = MetadataConfig.fromJson(metadataPath);
        return this;
    }

    public synchronized TaxProcessorBuilder withMetadata(String metadataJson) {
        metadataConfig = MetadataConfig.fromJson(metadataJson);
        return this;
    }

    public synchronized TaxProcessorBuilder withDelimiter(String delimiter) {
        if (delimiter == null || delimiter.length() != 1) {
            throw new IllegalArgumentException("delimiter must contain exactly one character");
        }
        metadataConfig = new MetadataConfig(
                delimiter,
                metadataConfig.getCharset(),
                metadataConfig.isLenientMode(),
                metadataConfig.getColumnMapping());
        return this;
    }

    public synchronized TaxProcessorBuilder enableLenientMode(boolean lenient) {
        metadataConfig = new MetadataConfig(
                metadataConfig.getCsvDelimiter(),
                metadataConfig.getCharset(),
                lenient,
                metadataConfig.getColumnMapping());
        return this;
    }

    public synchronized TaxProcessor build() {
        return new TaxProcessor(metadataConfig);
    }

    public synchronized TaxSummaryReport process(File csvFile) {
        return build().process(Objects.requireNonNull(csvFile, "csvFile must not be null"));
    }

    public synchronized TaxSummaryReport process(Path csvPath) {
        return build().process(Objects.requireNonNull(csvPath, "csvPath must not be null"));
    }

    public synchronized TaxSummaryReport process(InputStream csvInput) {
        return build().process(Objects.requireNonNull(csvInput, "csvInput must not be null"));
    }

    public synchronized TaxSummaryReport process(String csvContent) {
        return build().process(Objects.requireNonNull(csvContent, "csvContent must not be null"));
    }

    public synchronized TaxSummaryReport process(Reader csvReader) {
        return build().process(Objects.requireNonNull(csvReader, "csvReader must not be null"));
    }

    public synchronized String processToCsv(File csvFile) {
        return build().processToCsv(csvFile);
    }

    public synchronized String processToCsv(Path csvPath) {
        return build().processToCsv(csvPath);
    }

    public synchronized String processToCsv(InputStream csvInput) {
        return build().processToCsv(csvInput);
    }

    public synchronized String processToCsv(String csvContent) {
        return build().processToCsv(csvContent);
    }
}