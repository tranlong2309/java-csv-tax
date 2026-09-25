package com.company.taxlibrary;

import com.company.taxlibrary.model.TaxSummaryReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TaxProcessorTest {
    private static final String CSV = "mat_hang,so_luong,don_gia,phan_tram_vat\n"
            + "Ao so mi,2,100000,10\n"
            + "Laptop,1,15000000,0.1\n";

    @Test
    void processesMetadataAndCsvFilesEndToEnd(@TempDir Path tempDir) throws Exception {
        Path metadataFile = tempDir.resolve("metadata.json");
        Path csvFile = tempDir.resolve("tax.csv");
        Files.write(metadataFile, "{\"csvDelimiter\": \",\"}".getBytes(StandardCharsets.UTF_8));
        Files.write(csvFile, CSV.getBytes(StandardCharsets.UTF_8));

        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata(metadataFile.toFile())
                .process(csvFile.toFile());

        assertThat(report.getItemResults()).hasSize(2);
        assertThat(report.getGrandSubtotal()).isEqualByComparingTo("15200000.00");
        assertThat(report.getGrandTotalVat()).isEqualByComparingTo("35000.00");
        assertThat(report.getGrandTotalAmount()).isEqualByComparingTo("15235000.00");
        assertThat(report.getValidationWarnings()).isEmpty();
    }

    
    @Test
    void acceptsStringAndInputStreamSources() {
        TaxSummaryReport fromString = TaxProcessor.builder().process(CSV);
        TaxSummaryReport fromStream = TaxProcessor.builder().process(
                new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8)));

        assertThat(fromString.getGrandTotalAmount()).isEqualByComparingTo("15235000.00");
        assertThat(fromStream.getGrandTotalAmount()).isEqualByComparingTo("15235000.00");
    }

    @Test
    void producesEnrichedOutputCsv() {
        String output = TaxProcessor.builder().processToCsv(CSV);

        assertThat(output).startsWith("item_name,quantity,unit_price,vat_rate,subtotal,tien_vat,tong_thanh_toan");
        assertThat(output).contains("Ao so mi,2.0000,100000.0000,10.00,200000.00,20000.00,220000.00");
    }
}