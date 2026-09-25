# CSV Tax Extension

The `csv-tax-extension` is a domain-specific module built on top of the generic `csv-core` library. It implements the necessary business rules and algorithms to calculate VAT and aggregate subtotals and grand totals for product invoices.

## Features

- **Tax Calculations**: Accurately calculates VAT amounts and subtotals based on `quantity`, `unit price`, and `VAT rate`.
- **Grand Totals**: Aggregates the results into a single `TaxSummaryReport`.
- **Dynamic Headers**: Supports metadata configuration from a JSON string or file to dynamically map client-specific CSV headers (e.g., `Product Name` -> `itemName`) to internal model properties.
- **Lenient vs Strict Mode**: Ability to skip malformed rows with warnings (lenient mode) or fail fast (strict mode).
- **Zero Configuration**: Defaults to using standard Vietnamese tax columns (`mat_hang`, `so_luong`, `don_gia`, `phan_tram_vat`) if no metadata config is provided.
- **Security First**: All outputs are inherently sanitized by `csv-core` to prevent CSV Injection (`=`, `+`, `-`, `@`, `\t`, `\r`).

## Dependency

Ensure you have the `csv-core` library installed in your local Maven repository or remote Nexus before depending on this module.
```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>csv-tax-extension</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Basic Usage

The primary entry point is the `TaxProcessorBuilder`.

### 1. Process from File (Default Headers)
```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;
import java.io.File;

File csvInput = new File("invoices.csv");

// Process using default configuration
TaxSummaryReport report = TaxProcessor.builder().process(csvInput);

System.out.println("Grand Subtotal: " + report.getGrandSubtotal());
System.out.println("Total VAT: " + report.getGrandTotalVat());
System.out.println("Total Amount: " + report.getGrandTotalAmount());

// Inspect any non-fatal validation warnings
report.getValidationWarnings().forEach(warning -> {
    System.err.println("Line " + warning.getLineNumber() + ": " + warning.getMessage());
});
```

### 2. Custom Metadata Configuration
If your CSV file uses custom headers or a different delimiter (e.g. `;`), you can provide a JSON configuration:

**Metadata JSON (e.g. `metadata.json`):**
```json
{
  "csvDelimiter": ";",
  "lenientMode": true,
  "columnMapping": {
    "itemName": ["Product", "Item Name"],
    "quantity": ["Qty"],
    "unitPrice": ["Price", "Cost"],
    "vatRate": ["Tax %"]
  }
}
```

**Java Code:**
```java
File metadataJson = new File("metadata.json");

TaxSummaryReport report = TaxProcessor.builder()
    .withMetadata(metadataJson) // Applies custom headers and delimiter
    .process(new File("custom_invoices.csv"));
```

### 3. Generate Processed CSV Output
Instead of getting Java objects, you can output directly back to a CSV string containing all original data plus new columns (`subtotal`, `tien_vat`, `tong_thanh_toan`):

```java
String outputCsvString = TaxProcessor.builder()
    .processToCsv(new File("invoices.csv"));

// outputCsvString contains the fully processed and sanitized CSV content
```
