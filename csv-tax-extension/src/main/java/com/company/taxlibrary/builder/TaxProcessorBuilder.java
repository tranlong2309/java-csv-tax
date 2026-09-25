package com.company.taxlibrary.builder;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
public class TaxProcessorBuilder {
    private CsvColumnMappingConfig config = new CsvColumnMappingConfig(",", null, null, null);
    public TaxProcessorBuilder withMetadata(File file) { this.config = CsvColumnMappingConfig.fromJson(file); return this; }
    
    public TaxProcessorBuilder lenientMode() { return enableLenientMode(true); }
    public TaxProcessorBuilder enableLenientMode(boolean lenient) {
        this.config = new CsvColumnMappingConfig(config.getCsvDelimiter(), config.getCharset(), lenient, config.getColumnMapping());
        return this;
    }
    public TaxProcessorBuilder withDelimiter(String delimiter) {
        this.config = new CsvColumnMappingConfig(delimiter, config.getCharset(), config.isLenientMode(), config.getColumnMapping());
        return this;
    }
    public TaxProcessorBuilder withMetadata(Path file) { return withMetadata(file.toFile()); }
    public TaxProcessorBuilder withMetadata(String json) { this.config = CsvColumnMappingConfig.fromJson(json); return this; }

    
    public TaxProcessor build() { 
        if (config.getColumnMapping() == null || config.getColumnMapping().isEmpty()) {
            config = new CsvColumnMappingConfig(config.getCsvDelimiter(), config.getCharset(), config.isLenientMode(), com.company.taxlibrary.util.TaxHeaderHelper.getDefaultMapping());
        }
        return new TaxProcessor(config); 
    }

    public TaxSummaryReport process(File f) { return build().process(f); }
    public TaxSummaryReport process(Path p) { return build().process(p); }
    public TaxSummaryReport process(InputStream is) { return build().process(is); }
    
    public TaxSummaryReport process(String s) { return build().process(s); }
    public TaxSummaryReport process(java.io.Reader r) { return build().process(r); }

    
    public String processToCsv(String s) { return build().processToCsv(s); }
    public String processToCsv(File f) { return build().processToCsv(f); }
    public String processToCsv(Path p) { return build().processToCsv(p); }
    public String processToCsv(InputStream is) { return build().processToCsv(is); }

}
