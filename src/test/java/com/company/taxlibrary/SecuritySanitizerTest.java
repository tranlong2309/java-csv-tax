package com.company.taxlibrary;

import com.company.taxlibrary.config.MetadataConfig;
import com.company.taxlibrary.exception.InvalidCsvFormatException;
import com.company.taxlibrary.model.TaxItemInput;
import com.company.taxlibrary.parser.CsvReaderEngine;
import com.company.taxlibrary.parser.CsvWriterEngine;
import com.company.taxlibrary.util.CsvSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecuritySanitizerTest {
    @Test
    void sanitizerHandlesNullEmptySafeAndDangerousValues() {
        assertThat(CsvSanitizer.sanitize(null)).isEmpty();
        assertThat(CsvSanitizer.sanitize("")).isEmpty();
        assertThat(CsvSanitizer.sanitize("safe text")).isEqualTo("safe text");
        for (String payload : new String[]{"=formula", "+formula", "-formula", "@formula", "\tformula", "\rformula"}) {
            assertThat(CsvSanitizer.sanitize(payload)).isEqualTo("'" + payload);
        }
    }

    @Test
    void strictReaderRejectsMalformedRowsAndMissingHeaders() {
        MetadataConfig strict = new MetadataConfig(",", "UTF-8", false, null);
        CsvReaderEngine reader = new CsvReaderEngine(strict);

        assertThatThrownBy(() -> reader.read(
                new StringReader("mat_hang,so_luong,don_gia,phan_tram_vat\nonly,1\n"),
                row -> { },
                warning -> { }))
                .isInstanceOf(InvalidCsvFormatException.class)
                .hasMessageContaining("fewer fields");
        assertThatThrownBy(() -> reader.read(
                new StringReader("name,amount\nitem,1\n"), row -> { }, warning -> { }))
                .isInstanceOf(InvalidCsvFormatException.class)
                .hasMessageContaining("Missing required CSV columns");
    }

    @Test
    void tolerantReaderWarnsForEmptyAndCorruptRows() {
        AtomicInteger rows = new AtomicInteger();
        AtomicInteger warnings = new AtomicInteger();
        new CsvReaderEngine().read(new StringReader(""), row -> rows.incrementAndGet(), warning -> warnings.incrementAndGet());
        new CsvReaderEngine().read(
                new StringReader("mat_hang,so_luong,don_gia,phan_tram_vat\nitem,1\n"),
                row -> rows.incrementAndGet(),
                warning -> warnings.incrementAndGet());

        assertThat(rows).hasValue(0);
        assertThat(warnings).hasValue(2);

        AtomicInteger overloadRows = new AtomicInteger();
        new CsvReaderEngine().read(new StringReader(
                        "mat_hang,so_luong,don_gia,phan_tram_vat\nitem,1,100,10\n"),
                row -> overloadRows.incrementAndGet());
        assertThat(overloadRows).hasValue(1);
    }

    @Test
    void readerAndWriterValidateNullsAndDelimiterConfiguration() {
        CsvReaderEngine reader = new CsvReaderEngine();
        assertThatThrownBy(() -> reader.read((java.io.Reader) null, row -> { }, warning -> { }))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> reader.read(new StringReader(""), null, warning -> { }))
                .isInstanceOf(NullPointerException.class);
        MetadataConfig invalidDelimiter = new MetadataConfig(",,", "UTF-8", true, null);
        assertThatThrownBy(() -> new CsvReaderEngine(invalidDelimiter)
                .read(new StringReader("a\n"), row -> { }, warning -> { }))
                .isInstanceOf(InvalidCsvFormatException.class);
        assertThatThrownBy(() -> reader.read((java.io.File) null, row -> { }, warning -> { }))
                .isInstanceOf(NullPointerException.class);

        CsvWriterEngine writer = new CsvWriterEngine();
        assertThatThrownBy(() -> writer.write(null, new StringWriter(), ','))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.write(new StringReader("a"), null, ','))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> writer.write(Collections.emptyList(), new StringWriter(), null, ','))
                .isInstanceOf(NullPointerException.class);
        assertThatCode(() -> new CsvReaderEngine(null).read(
                new StringReader("mat_hang,so_luong,don_gia,phan_tram_vat\nitem,1,100,10\n"),
                row -> { }))
                .doesNotThrowAnyException();
    }

    @Test
    void writerSanitizesHeadersValuesAndSupportsFiles(@TempDir Path tempDir) throws Exception {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("name", "=SUM(A1:A2)");
        values.put("empty", null);
        TaxItemInput row = new TaxItemInput(1, values);
        StringWriter output = new StringWriter();
        new CsvWriterEngine().write(Collections.singletonList(row), output,
                java.util.Arrays.asList("name", "empty"), ',');
        assertThat(output.toString()).contains("'=SUM(A1:A2)");

        Path input = tempDir.resolve("input.csv");
        Files.write(input, "name,amount\n@bad,1\n".getBytes(StandardCharsets.UTF_8));
        AtomicInteger count = new AtomicInteger();
        new CsvReaderEngine().read(input.toFile(), rowValue -> count.incrementAndGet(), warning -> { });
        assertThat(count).hasValue(1);
    }

        @Test
        void writerAndReaderWrapIoFailures(@TempDir Path tempDir) {
                Writer failingWriter = new Writer() {
                        @Override
                        public void write(char[] chars, int offset, int length) throws IOException {
                                throw new IOException("write failure");
                        }

                        @Override
                        public void flush() throws IOException {
                                throw new IOException("flush failure");
                        }

                        @Override
                        public void close() throws IOException {
                                throw new IOException("close failure");
                        }
                };
                CsvWriterEngine writer = new CsvWriterEngine();
                assertThatThrownBy(() -> writer.write(new StringReader("a\nb\n"), failingWriter, ','))
                                .isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> writer.write(Collections.emptyList(), failingWriter,
                                Collections.singletonList("value"), ','))
                                .isInstanceOf(IllegalStateException.class);
                assertThatThrownBy(() -> writer.writeEnriched(Collections.emptyList(), failingWriter, ','))
                                .isInstanceOf(IllegalStateException.class);

                java.io.File missing = tempDir.resolve("missing.csv").toFile();
                assertThatThrownBy(() -> new CsvReaderEngine().read(missing, row -> { }, warning -> { }))
                                .isInstanceOf(InvalidCsvFormatException.class);
                java.io.Reader failingReader = new java.io.Reader() {
                        @Override
                        public int read(char[] chars, int offset, int length) throws IOException {
                                throw new IOException("read failure");
                        }

                        @Override
                        public void close() {
                        }
                };
                assertThatThrownBy(() -> new CsvReaderEngine().read(failingReader, row -> { }, warning -> { }))
                                .isInstanceOf(InvalidCsvFormatException.class);
        }
}
