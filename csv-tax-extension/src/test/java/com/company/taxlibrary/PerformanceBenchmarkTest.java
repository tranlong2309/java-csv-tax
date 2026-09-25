package com.company.taxlibrary;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.nio.file.Files;

public class PerformanceBenchmarkTest {

    @Test
    void testPerformanceIsWithinSla() throws Exception {
        File tempFile = File.createTempFile("perf-", ".csv");
        tempFile.deleteOnExit();
        Files.writeString(tempFile.toPath(), "Item,Qty,Price,Tax\nTest,1,100,10%");
        
        long start = System.currentTimeMillis();
        TaxProcessor processor = TaxProcessor.builder().build();
        processor.process(tempFile);
        long duration = System.currentTimeMillis() - start;
        
        // Ensure that processing a simple file is extremely fast (under 1 second)
        assertThat(duration).isLessThan(1000); 
    }
}
