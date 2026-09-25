package com.company.csvengine.parser;

import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.model.ValidationWarning;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

class CsvEngineTest {
    @Test
    void readerStreamsRowsAndWarnsForShortRowsInLenientMode() {
        String csv = "h1,h2,h3\nv1,v2,v3\nshort1,short2";
        CsvColumnMappingConfig config = new CsvColumnMappingConfig(",", "UTF-8", true, Collections.emptyMap());
        CsvReaderEngine engine = new CsvReaderEngine(config);
        
        List<Map<String, String>> rows = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();
        
        engine.read(new StringReader(csv), rows::add, warnings::add);
        
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("h1", "v1").containsEntry("h2", "v2").containsEntry("h3", "v3");
        
        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).getCode()).isEqualTo("CSV_ROW_TOO_SHORT");
    }

    @Test
    void writerSanitizesRawValuesBeforeCsvEscaping() {
        CsvWriterEngine engine = new CsvWriterEngine();
        StringWriter writer = new StringWriter();
        
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", "=SUM(A1:B1)"); // malicious
        row.put("desc", "hello");
        
        engine.write(List.of(row), writer, List.of("name", "desc"), ',');
        
        String output = writer.toString();
        // CsvSanitizer adds a single quote prefix to '='
        assertThat(output).contains("'=SUM(A1:B1)"); // depending on commons-csv escaping, but wait, CsvSanitizer adds single quote.
    }
    
    @Test
    void writeWithMetadataExportsPreferredHeaders() {
        Map<String, List<String>> mapping = new LinkedHashMap<>();
        mapping.put("title", List.of("Tiêu đề", "Title"));
        CsvColumnMappingConfig config = new CsvColumnMappingConfig(",", "UTF-8", true, mapping);
        
        CsvWriterEngine engine = new CsvWriterEngine();
        StringWriter writer = new StringWriter();
        
        Map<String, String> row = new LinkedHashMap<>();
        row.put("title", "abc");
        
        engine.write(List.of(row), writer, List.of("title"), config, ',');
        
        assertThat(writer.toString()).startsWith("Tiêu đề");
        assertThat(writer.toString()).contains("abc");
    }
    
    @Test
    void canBeInstantiatedWithDefaultConstructorAndReadCsv() {
        CsvReaderEngine engine = new CsvReaderEngine();
        String csv = "a,b,c\n1,2,3";
        java.util.List<Map<String, String>> rows = new java.util.ArrayList<>();
        engine.read(new StringReader(csv), rows::add);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("a", "1").containsEntry("b", "2").containsEntry("c", "3");
    }
}
