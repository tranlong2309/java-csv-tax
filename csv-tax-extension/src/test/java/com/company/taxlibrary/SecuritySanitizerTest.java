package com.company.taxlibrary;

import com.company.csvengine.exception.InvalidCsvFormatException;
import com.company.taxlibrary.model.TaxSummaryReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SecuritySanitizerTest {

    @Test
    void mitigatesCsvInjectionAndSanitizesOutput() {
        String inputCsv = "mat_hang,so_luong,don_gia,phan_tram_vat\n"
                + "=CMD(),1,100,10\n"
                + "+CMD(),1,100,10\n"
                + "-CMD(),1,100,10\n"
                + "@CMD(),1,100,10\n"
                + "\tCMD(),1,100,10\n";
        
        String outputCsv = TaxProcessor.builder().processToCsv(inputCsv);
        
        // Assert that the malicious characters are prefixed with '
        assertThat(outputCsv).contains("'=CMD()");
        assertThat(outputCsv).contains("'+CMD()");
        assertThat(outputCsv).contains("'-CMD()");
        assertThat(outputCsv).contains("'@CMD()");
        assertThat(outputCsv).contains("'\tCMD()");
        
        assertThat(com.company.csvengine.util.CsvSanitizer.sanitize("\rCMD()")).isEqualTo("'\rCMD()");
    }

    @Test
    void nullGuardsForProcessMethods() {
        TaxProcessor processor = TaxProcessor.builder().build();
        assertThatThrownBy(() -> processor.process((File) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("File must not be null");
        assertThatThrownBy(() -> processor.process((Path) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> processor.process((InputStream) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("InputStream must not be null");
        assertThatThrownBy(() -> processor.process((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("csvContent must not be null");
        assertThatThrownBy(() -> processor.process((Reader) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Reader must not be null");
    }

    @Test
    void strictModeRejectsInvalidRowsAndHeaders() {
        String invalidRowCsv = "mat_hang,so_luong,don_gia,phan_tram_vat\nValid,1,100,10\nInvalidOnlyTwoCols,1\n";
        
        assertThatThrownBy(() -> TaxProcessor.builder().enableLenientMode(false).process(invalidRowCsv))
                .isInstanceOf(InvalidCsvFormatException.class)
                .hasMessageContaining("Row has fewer columns than header");

        String missingHeaderCsv = "so_luong,don_gia,phan_tram_vat\n1,100,10\n";
        
        assertThatThrownBy(() -> TaxProcessor.builder().enableLenientMode(false).process(missingHeaderCsv))
                .isInstanceOf(InvalidCsvFormatException.class)
                .hasMessageContaining("Missing required header for field: itemName");
    }

    @Test
    void ioFailureIsWrappedInInvalidCsvFormatException(@TempDir Path tempDir) {
        Path missingFile = tempDir.resolve("missing_file.csv");
        
        assertThatThrownBy(() -> TaxProcessor.builder().process(missingFile))
                .isInstanceOf(InvalidCsvFormatException.class)
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    void readsAndWritesRealFiles(@TempDir Path tempDir) throws Exception {
        Path inputPath = tempDir.resolve("input.csv");
        Files.writeString(inputPath, "mat_hang,so_luong,don_gia,phan_tram_vat\nApples,2,50,10\n");

        TaxSummaryReport report = TaxProcessor.builder().process(inputPath.toFile());
        assertThat(report.getGrandTotalAmount()).isEqualByComparingTo("110.00");
        
        String outputCsv = TaxProcessor.builder().processToCsv(inputPath.toFile());
        assertThat(outputCsv).contains("Apples");
        assertThat(outputCsv).contains("110.00");
    }
}
