package com.company.taxlibrary.builder;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
public class TaxProcessorBuilder {
    private CsvColumnMappingConfig config = new CsvColumnMappingConfig(null, null, null, null);
    public TaxProcessorBuilder withMetadata(File file) { this.config = CsvColumnMappingConfig.fromJson(file); return this; }
    public TaxProcessorBuilder lenientMode() { return this; } // stub
    public TaxProcessor build() { return new TaxProcessor(config); }
    public TaxSummaryReport process(File f) { return build().process(f); }
    public TaxSummaryReport process(Path p) { return build().process(p); }
    public TaxSummaryReport process(InputStream is) { return build().process(is); }
    public TaxSummaryReport process(String s) { return build().process(s); }
    public String processToCsv(String s) { return build().processToCsv(s); }
}
