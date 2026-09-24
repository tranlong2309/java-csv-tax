import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TaxProcessorDemo {
    private TaxProcessorDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path projectDirectory = Path.of(".");
        Path examplesDirectory = projectDirectory.resolve("examples");
        Path input = examplesDirectory.resolve("custom-input.csv");
        Path metadata = examplesDirectory.resolve("metadata-custom.json");

        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata(new File(metadata.toString()))
                .withDelimiter(";")
                .process(input.toFile());

        System.out.println("Tổng trước thuế: " + report.getGrandSubtotal());
        System.out.println("Tổng VAT: " + report.getGrandTotalVat());
        System.out.println("Tổng thanh toán: " + report.getGrandTotalAmount());
        System.out.println("Số dòng hợp lệ: " + report.getItemResults().size());
        System.out.println("Số cảnh báo: " + report.getValidationWarnings().size());

        String outputCsv = TaxProcessor.builder()
            .processToCsv(examplesDirectory.resolve("tax-input.csv").toFile());
        Files.write(
                examplesDirectory.resolve("tax-output.csv"),
                outputCsv.getBytes(StandardCharsets.UTF_8));
        System.out.println("Đã ghi: examples/tax-output.csv");
    }
}
