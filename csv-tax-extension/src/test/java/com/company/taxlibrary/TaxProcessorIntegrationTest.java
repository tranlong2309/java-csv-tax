package com.company.taxlibrary;

import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.exception.InvalidCsvFormatException;
import com.company.csvengine.exception.CsvColumnMappingConfigException;
import com.company.csvengine.exception.TaxProcessingException;
import com.company.taxlibrary.model.TaxItemInput;
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
        Files.write(metadataPath, "{}".getBytes(StandardCharsets.UTF_8));
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
                + "\"columnMapping\":{\"itemNameHeader\":[\"Product\"],"
                + "\"quantityHeader\":[\"Qty\"],\"unitPriceHeader\":[\"Price\"],"
                + "\"vatRateHeader\":[\"Tax\"]}}";
        String csv = "Product;Qty;Price;Tax\nCustom;2;100;10%\n";

        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata(metadata)
                .withDelimiter(";")
                .enableLenientMode(true)
                .process(csv);

        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("200.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("20.00");
        assertThat(report.getValidationWarnings()).isEmpty();
    }

        @Test
        void coversImmutableDtoAndExceptionAccessors() {
                TaxItemInput input = new TaxItemInput(7, java.util.Collections.singletonMap("name", "value"));
                assertThat(input.getLineNumber()).isEqualTo(7);
                assertThat(input.getRawDataMap()).containsEntry("name", "value");

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
                assertThat(new TaxProcessingException("message").getMessage()).isEqualTo("message");
                assertThat(new TaxProcessingException(cause).getCause()).isSameAs(cause);
                assertThat(new TaxProcessingException("message", cause).getCause()).isSameAs(cause);
                assertThat(new InvalidCsvFormatException("invalid").getMessage()).isEqualTo("invalid");
                assertThat(new InvalidCsvFormatException(cause).getCause()).isSameAs(cause);
                assertThat(new InvalidCsvFormatException("invalid", cause).getCause()).isSameAs(cause);
                assertThat(new CsvColumnMappingConfigException("metadata").getMessage()).isEqualTo("metadata");
                assertThat(new CsvColumnMappingConfigException("metadata", cause).getCause()).isSameAs(cause);
        }

    @Test
    void handlesLenientAndStrictInvalidRows() {
        String invalid = "mat_hang,so_luong,don_gia,phan_tram_vat\nBad,not-a-number,100,10\n";
        TaxSummaryReport lenient = TaxProcessor.builder().enableLenientMode(true).process(invalid);
        assertThat(lenient.getItemResults()).isEmpty();
        assertThat(lenient.getValidationWarnings()).singleElement()
                .extracting(warning -> warning.getCode())
                .isEqualTo("INVALID_ROW");

        assertThatThrownBy(() -> TaxProcessor.builder().enableLenientMode(false).process(invalid))
                .isInstanceOf(InvalidCsvFormatException.class);

        TaxSummaryReport missingValue = TaxProcessor.builder().process(
                "mat_hang,so_luong,don_gia,phan_tram_vat\n,1,100,10\n");
        assertThat(missingValue.getValidationWarnings()).singleElement()
                .extracting(warning -> warning.getCode()).isEqualTo("INVALID_ROW");
        TaxSummaryReport missingColumn = TaxProcessor.builder().process(
                "other,so_luong,don_gia,phan_tram_vat\nitem,1,100,10\n");
        assertThat(missingColumn.getValidationWarnings()).hasSize(2);
    }

    @Test
    void handlesEmptyInputAndPublicNullGuards() {
        TaxSummaryReport empty = TaxProcessor.builder().process("");
        assertThat(empty.getItemResults()).isEmpty();
        assertThat(empty.getValidationWarnings()).isNotEmpty();

        assertThatThrownBy(() -> TaxProcessor.builder().withMetadata((String) null))
                .isInstanceOf(CsvColumnMappingConfigException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().withDelimiter(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().withDelimiter(",,"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().process((String) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().process((Path) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().process((java.io.InputStream) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> TaxProcessor.builder().process((StringReader) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaxProcessor(null))
                .isInstanceOf(NullPointerException.class);
        CsvColumnMappingConfig invalidDelimiter = new CsvColumnMappingConfig(",,", "UTF-8", true, null);
        assertThatThrownBy(() -> new TaxProcessor(invalidDelimiter).process(CSV))
                .isInstanceOf(InvalidCsvFormatException.class);
        assertThatThrownBy(() -> new TaxProcessor(invalidDelimiter).processToCsv(CSV))
                .isInstanceOf(InvalidCsvFormatException.class);
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
