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

    public TaxSummaryReport process(File csvFile) { return TaxCalculator.calculateGrandTotals(buildProcessor().process(csvFile)); }
    public TaxSummaryReport process(Path csvPath) { return TaxCalculator.calculateGrandTotals(buildProcessor().process(csvPath.toFile())); }
    public TaxSummaryReport process(InputStream csvInput) { return TaxCalculator.calculateGrandTotals(buildProcessor().process(csvInput)); }
    public TaxSummaryReport process(String csvContent) { return TaxCalculator.calculateGrandTotals(buildProcessor().process(new java.io.StringReader(csvContent))); }

    public String processToCsv(String csvContent) {
        List<TaxItemOutput> results = buildProcessor().process(new java.io.StringReader(csvContent));
        java.io.StringWriter writer = new java.io.StringWriter();
        List<java.util.Map<String, String>> rows = new java.util.ArrayList<>();
        for (TaxItemOutput r : results) {
            java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
            map.put("item_name", r.getItemName());
            map.put("quantity", r.getQuantity().toPlainString());
            map.put("unit_price", r.getUnitPrice().toPlainString());
            map.put("vat_rate", r.getVatRate().toPlainString());
            map.put("subtotal", r.getSubtotal().toPlainString());
            map.put("tien_vat", r.getVatAmount().toPlainString());
            map.put("tong_thanh_toan", r.getTotalAmount().toPlainString());
            rows.add(map);
        }
        new com.company.csvengine.parser.CsvWriterEngine().write(rows, writer, 
            java.util.List.of("item_name", "quantity", "unit_price", "vat_rate", "subtotal", "tien_vat", "tong_thanh_toan"), 
            config.getCsvDelimiter().charAt(0));
        return writer.toString();
    }

    private CsvProcessor<TaxItemOutput> buildProcessor() {
        return CsvProcessor.<TaxItemOutput>builder().withMetadata(config).withCalculator(new TaxCalculator()).build();
    }
}
