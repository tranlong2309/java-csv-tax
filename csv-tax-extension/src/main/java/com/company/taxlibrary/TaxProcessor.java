package com.company.taxlibrary;
import com.company.csvengine.CsvProcessor;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.taxlibrary.builder.TaxProcessorBuilder;
import com.company.taxlibrary.model.TaxItemOutput;
import com.company.taxlibrary.model.TaxSummaryReport;
import com.company.taxlibrary.service.TaxCalculator;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class TaxProcessor {
    private final CsvColumnMappingConfig config;
    public TaxProcessor(CsvColumnMappingConfig config) { this.config = config; }
    public static TaxProcessorBuilder builder() { return new TaxProcessorBuilder(); }

    
    public TaxSummaryReport process(java.io.Reader reader) {
        java.util.Objects.requireNonNull(reader, "Reader must not be null");
        java.util.List<com.company.csvengine.model.ValidationWarning> warnings = new java.util.ArrayList<>();
        long start = System.currentTimeMillis();
        List<TaxItemOutput> items = buildProcessor().process(reader, warnings::add);
        long time = System.currentTimeMillis() - start;
        TaxSummaryReport report = TaxCalculator.calculateGrandTotals(items);
        return new TaxSummaryReport(report.getGrandSubtotal(), report.getGrandTotalVat(), report.getGrandTotalAmount(), report.getItemResults(), warnings, time);
    }
    public TaxSummaryReport process(File csvFile) {
        java.util.Objects.requireNonNull(csvFile, "File must not be null");
        try (java.io.Reader r = java.nio.file.Files.newBufferedReader(csvFile.toPath(), java.nio.charset.Charset.forName(config.getCharset()))) { return process(r); } catch (com.company.csvengine.exception.CsvProcessingException e) { throw e; } catch (Exception e) { throw new com.company.csvengine.exception.InvalidCsvFormatException(e); }
    }

    public TaxSummaryReport process(Path csvPath) { return process(csvPath.toFile()); }
    
    public TaxSummaryReport process(InputStream csvInput) {
        java.util.Objects.requireNonNull(csvInput, "InputStream must not be null");
        try (java.io.Reader r = new java.io.InputStreamReader(csvInput, java.nio.charset.Charset.forName(config.getCharset()))) { return process(r); } catch (com.company.csvengine.exception.CsvProcessingException e) { throw e; } catch (Exception e) { throw new com.company.csvengine.exception.InvalidCsvFormatException(e); }
    }

    public TaxSummaryReport process(String csvContent) {
        java.util.Objects.requireNonNull(csvContent, "csvContent must not be null");
        return process(new java.io.StringReader(csvContent));
    }

    
    public String processToCsv(File csvFile) {
        java.util.Objects.requireNonNull(csvFile, "csvFile must not be null");
        try { return processToCsv(new String(java.nio.file.Files.readAllBytes(csvFile.toPath()), "UTF-8")); } catch (com.company.csvengine.exception.CsvProcessingException e) { throw e; } catch (Exception e) { throw new com.company.csvengine.exception.InvalidCsvFormatException(e); }
    }
    public String processToCsv(Path csvPath) { return processToCsv(csvPath.toFile()); }
    public String processToCsv(InputStream csvInput) {
        java.util.Objects.requireNonNull(csvInput, "csvInput must not be null");
        try { return processToCsv(new String(csvInput.readAllBytes(), "UTF-8")); } catch (com.company.csvengine.exception.CsvProcessingException e) { throw e; } catch (Exception e) { throw new com.company.csvengine.exception.InvalidCsvFormatException(e); }
    }
    
    public String processToCsv(String csvContent) {
        java.util.Objects.requireNonNull(csvContent, "csvContent must not be null");
        List<TaxItemOutput> results = buildProcessor().process(new java.io.StringReader(csvContent));
        java.io.StringWriter writer = new java.io.StringWriter();
        List<java.util.Map<String, String>> rows = new java.util.ArrayList<>();
        for (TaxItemOutput r : results) {
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            map.put("item_name", r.getItemName());
            map.put("quantity", String.format(java.util.Locale.US, "%.4f", r.getQuantity()));
            map.put("unit_price", String.format(java.util.Locale.US, "%.4f", r.getUnitPrice()));
            map.put("vat_rate", String.format(java.util.Locale.US, "%.2f", r.getVatRate()));
            map.put("subtotal", String.format(java.util.Locale.US, "%.2f", r.getSubtotal()));
            map.put("tien_vat", String.format(java.util.Locale.US, "%.2f", r.getVatAmount()));
            map.put("tong_thanh_toan", String.format(java.util.Locale.US, "%.2f", r.getTotalAmount()));
            rows.add(map);
        }
        new com.company.csvengine.parser.CsvWriterEngine().write(rows, writer, 
            java.util.List.of("item_name", "quantity", "unit_price", "vat_rate", "subtotal", "tien_vat", "tong_thanh_toan"), 
            config.getCsvDelimiter().charAt(0));
        return writer.toString();
    }

    private CsvProcessor<TaxItemOutput> buildProcessor() {
        return CsvProcessor.<TaxItemOutput>builder().withMetadata(config).withRowCalculator(new TaxCalculator()).build();
    }
}
