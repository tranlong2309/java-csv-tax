package com.company.taxlibrary.parser;

import com.company.taxlibrary.model.TaxItemInput;
import com.company.taxlibrary.model.ValidationWarning;
import com.company.taxlibrary.util.CsvSanitizer;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CsvEngineTest {
    @Test
    void sanitizesEveryFormulaPrefix() {
        assertThat(CsvSanitizer.sanitize("=SUM(A1:A2)")).isEqualTo("'=SUM(A1:A2)");
        assertThat(CsvSanitizer.sanitize("+danger")).isEqualTo("'+danger");
        assertThat(CsvSanitizer.sanitize("-danger")).isEqualTo("'-danger");
        assertThat(CsvSanitizer.sanitize("@danger")).isEqualTo("'@danger");
        assertThat(CsvSanitizer.sanitize("\tdanger")).isEqualTo("'\tdanger");
        assertThat(CsvSanitizer.sanitize("\rdanger")).isEqualTo("'\rdanger");
    }

    @Test
    void writerSanitizesRawValuesBeforeCsvEscaping() {
        StringWriter output = new StringWriter();
        new CsvWriterEngine().write(
                new StringReader("name,amount\n=SUM(A1:A2),10\n"), output, ',');

        assertThat(output.toString()).contains("'=SUM(A1:A2),10");
    }

    @Test
    void readerStreamsRowsAndWarnsForShortRowsInLenientMode() {
        StringBuilder csv = new StringBuilder("mat_hang,so_luong,don_gia,phan_tram_vat\n");
        for (int index = 0; index < 10000; index++) {
            csv.append("Item ").append(index).append(",1,100,10\n");
        }
        csv.append("bad,1\n");

        AtomicInteger rowCount = new AtomicInteger();
        List<ValidationWarning> warnings = new ArrayList<>();
        new CsvReaderEngine().read(
                new StringReader(csv.toString()),
                row -> rowCount.incrementAndGet(),
                warnings::add);

        assertThat(rowCount).hasValue(10000);
        assertThat(warnings).singleElement()
                .extracting(ValidationWarning::getCode)
                .isEqualTo("CSV_ROW_TOO_SHORT");
    }

    @Test
    void writerStreamsInputRowsUsingProvidedHeaders() {
        List<TaxItemInput> rows = new ArrayList<>();
        rows.add(new TaxItemInput(1, java.util.Collections.singletonMap("name", "=SUM(A1:A2)")));

        StringWriter output = new StringWriter();
        new CsvWriterEngine().write(rows, output, java.util.Collections.singletonList("name"), ',');

        assertThat(output.toString()).contains("'=SUM(A1:A2)");
    }
}