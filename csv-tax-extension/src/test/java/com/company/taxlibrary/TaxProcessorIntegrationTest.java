package com.company.taxlibrary;

import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.exception.InvalidCsvFormatException;
import com.company.csvengine.exception.CsvColumnMappingConfigException;
import com.company.csvengine.exception.CsvProcessingException;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import com.company.csvengine.model.ValidationWarning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxProcessorIntegrationTest {
    private static final String CSV = "mat_hang,so_luong,don_gia,phan_tram_vat\n"
            + "Item,2,100,10\n";

    @Test
    void processesEverySupportedSourceAndOutputOverload(@TempDir Path tempDir) throws Exception {
        Path csvPath = tempDir.resolve("input.csv");
        Path metadataPath = tempDir.resolve("metadata.json");
        Files.write(csvPath, CSV.getBytes(StandardCharsets.UTF_8));
        Files.write(metadataPath, "{\"csvDelimiter\": \",\"}".getBytes(StandardCharsets.UTF_8));
        TaxProcessor processor = TaxProcessor.builder().build();

        assertThat(processor.process(csvPath).getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(processor.process(csvPath.toFile()).getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(processor.process(CSV).getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(processor.process(new StringReader(CSV)).getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(processor.process(new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8)))
                .getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(TaxProcessor.builder().process(new StringReader(CSV)).getGrandTotalAmount())
                .isEqualByComparingTo("220.00");

        assertThat(processor.processToCsv(csvPath)).contains("tien_vat", "tong_thanh_toan");
        assertThat(processor.processToCsv(csvPath.toFile())).contains("220.00");
        assertThat(processor.processToCsv(CSV)).contains("220.00");
        assertThat(processor.processToCsv(new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8))))
                .contains("220.00");
        assertThat(TaxProcessor.builder().processToCsv(csvPath).length()).isPositive();
        assertThat(TaxProcessor.builder().processToCsv(csvPath.toFile()).length()).isPositive();
        assertThat(TaxProcessor.builder().processToCsv(
                new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8))).length()).isPositive();
        assertThat(TaxProcessor.builder().withMetadata(metadataPath).process(CSV)
                .getGrandTotalAmount()).isEqualByComparingTo("220.00");
        assertThat(TaxProcessor.builder().process(csvPath).getGrandTotalAmount())
                .isEqualByComparingTo("220.00");
    }

    
    @Test
    void supportsCustomMetadataDelimiterAndBuilderOptions() {
        String metadata = "{\"csvDelimiter\":\";\",\"lenientMode\":true,"
                + "\"columnMapping\":{\"itemName\":[\"Product\"],"
                + "\"quantity\":[\"Qty\"],\"unitPrice\":[\"Price\"],"
                + "\"vatRate\":[\"Tax\"]}}";
        String csv = "Product;Qty;Price;Tax\nCustom;2;100;10%\n";

        TaxSummaryReport report = TaxProcessor.builder()
                .withDelimiter(";")
                .withMetadata(metadata)
                .enableLenientMode(true)
                .process(csv);

        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("200.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("20.00");
        assertThat(report.getValidationWarnings()).isEmpty();
    }

        @Test
        void coversImmutableDtoAndExceptionAccessors() {

                ValidationWarning warning = new ValidationWarning(7, "CODE", "message");
                assertThat(warning.getLineNumber()).isEqualTo(7);
                assertThat(warning.getCode()).isEqualTo("CODE");
                assertThat(warning.getMessage()).isEqualTo("message");

                TaxItemOutput output = new TaxItemOutput(
                                7, "item", BigDecimal.ONE, BigDecimal.TEN,
                                BigDecimal.ZERO, BigDecimal.TEN,
                                BigDecimal.ZERO, BigDecimal.TEN, true,
                                java.util.Collections.singletonList("note"));
                TaxSummaryReport report = new TaxSummaryReport(
                                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                                java.util.Collections.singletonList(output),
                                java.util.Collections.singletonList(warning), 12L);
                assertThat(report.getProcessingTimeMs()).isEqualTo(12L);

                Throwable cause = new IllegalStateException("cause");
                assertThat(new CsvProcessingException("message").getMessage()).isEqualTo("message");
                assertThat(new CsvProcessingException(cause).getCause()).isSameAs(cause);
                assertThat(new CsvProcessingException("message", cause).getCause()).isSameAs(cause);
                assertThat(new InvalidCsvFormatException("invalid").getMessage()).isEqualTo("invalid");
                assertThat(new InvalidCsvFormatException(cause).getCause()).isSameAs(cause);
                assertThat(new InvalidCsvFormatException("invalid", cause).getCause()).isSameAs(cause);
                assertThat(new CsvColumnMappingConfigException("metadata").getMessage()).isEqualTo("metadata");
                assertThat(new CsvColumnMappingConfigException("metadata", cause).getCause()).isSameAs(cause);
        }

    
    @Test
    void handlesLenientAndStrictInvalidRows() {
        String shortRow = "mat_hang,so_luong,don_gia,phan_tram_vat\nBad,1\n";
        TaxSummaryReport lenient = TaxProcessor.builder().process(shortRow);
        assertThat(lenient.getItemResults()).isEmpty();
        assertThat(lenient.getValidationWarnings()).singleElement()
                .extracting(warning -> warning.getCode())
                .isEqualTo("CSV_ROW_TOO_SHORT");
    }
    @Test
    void handlesEmptyInputAndPublicNullGuards() {
        TaxSummaryReport empty = TaxProcessor.builder().process("");
        assertThat(empty.getItemResults()).isEmpty();
        
        assertThatThrownBy(() -> TaxProcessor.builder().withMetadata((String) null))
                .isInstanceOf(com.company.csvengine.exception.CsvColumnMappingConfigException.class);
    }
    @Test
    void reportsMissingFilesAndCanBeUsedConcurrently(@TempDir Path tempDir) throws Exception {
        Path missing = tempDir.resolve("missing.csv");
        assertThatThrownBy(() -> TaxProcessor.builder().process(missing))
                .isInstanceOf(InvalidCsvFormatException.class);

        TaxProcessor processor = TaxProcessor.builder().build();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<TaxSummaryReport>> tasks = new ArrayList<>();
            for (int index = 0; index < 8; index++) {
                tasks.add(() -> processor.process(CSV));
            }
            List<Future<TaxSummaryReport>> results = executor.invokeAll(tasks);
            for (Future<TaxSummaryReport> result : results) {
                assertThat(result.get().getGrandTotalAmount()).isEqualByComparingTo("220.00");
            }
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

        @Test
        void wrapsInputStreamCloseFailures() {
                java.io.InputStream failing = new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8)) {
                        @Override
                        public void close() throws java.io.IOException {
                                throw new java.io.IOException("close failure");
                        }
                };
                assertThatThrownBy(() -> TaxProcessor.builder().process(failing))
                                .isInstanceOf(InvalidCsvFormatException.class);
        }
}
