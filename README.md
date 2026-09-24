# Shared CSV Tax Library

A high-performance, streaming-based Java 17 library for parsing CSV tax records, computing exact VAT financials, mapping columns flexibly via JSON metadata, and securely generating detailed tax reports without risking memory exhaustion.

## 🚀 Quick Start (5-Minute Example)

### 1. Add Maven Dependency

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>shared-csv-tax-library</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Run the End-to-End Code

Create a simple input file named `tax-input.csv`:
```csv
item_name, quantity, unit_price, vat_rate
MacBook Pro, 1, 2000.00, 10%
Mechanical Keyboard, 2, 150.00, 5%
```

Run the `TaxProcessor`:
```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.io.File;

public class TaxExample {
    public static void main(String[] args) {
        // Build processor (Thread-Safe & Immutable)
        TaxProcessor processor = TaxProcessor.builder()
                .enableLenientMode(true) // Continues processing even if a row fails
                .build();
        
        // Process the CSV end-to-end
        TaxSummaryReport report = processor.process(new File("tax-input.csv"));
        
        // Print Output
        System.out.println("Subtotal: $" + report.getGrandSubtotal());
        System.out.println("Total VAT: $" + report.getGrandTotalVat());
        System.out.println("Grand Total: $" + report.getGrandTotalAmount());
        
        // Output detailed item rows
        report.getItemResults().forEach(item -> {
            System.out.println(item.getItemName() + " -> Total: $" + item.getTotalAmount());
        });
    }
}
```

## 🏗 Library Architecture

The library rigorously adheres to Clean Code and SOLID principles:
- **`TaxProcessor` (Facade)**: The public thread-safe entry point for clients.
- **Fluent Builder**: `TaxProcessorBuilder` dynamically mounts configurations before instantiation.
- **`CsvReaderEngine` & `CsvWriterEngine` (Encapsulated)**: Strictly internal parsers leveraging Apache Commons CSV for streaming I/O without memory bloat.
- **Immutable Domain Models**: All reports (`TaxSummaryReport`, `TaxItemOutput`) and configurations (`MetadataConfig`) are heavily shielded using `java.lang.Record` and `Collections.unmodifiableMap()`, preventing external tampering.

## 🔌 Fluent API & I/O Support

The API flexibly accepts `File`, `Path`, `InputStream`, `String`, or `Reader`. All stream resources natively employ `try-with-resources` ensuring zero memory leaks or dangling file locks.

```java
TaxProcessor processor = TaxProcessor.builder()
        .withDelimiter(";")
        .withMetadata(new File("metadata.json"))
        .build();

// Execute using distinct inputs
processor.process(Path.of("data.csv"));
processor.process("item,qty,price,vat\nLaptop,1,1000,10%");
```

## 📝 JSON Metadata Setup

If your CSV headers deviate from standard defaults, you can dynamically map them via a JSON Metadata Config file:

**`metadata.json`**:
```json
{
  "csvDelimiter": ",",
  "charset": "UTF-8",
  "lenientMode": true,
  "columnMapping": {
    "itemNameHeader": ["Product", "Tên Hàng"],
    "quantityHeader": ["Qty", "Số Lượng"],
    "unitPriceHeader": ["Price", "Đơn Giá"],
    "vatRateHeader": ["Tax", "VAT"]
  }
}
```
*Note: Header normalization natively strips UTF-8 BOM, trims whitespace, standardizes casing, and safely sanitizes Vietnamese diacritics.*

## 🛡 Security Features

- **Formula Injection Defense**: All generated CSV output cells starting with dangerous Excel payloads (`=`, `+`, `-`, `@`, `\t`, `\r`) are safely neutralized by prepending a single quote (`'`).
- **Path Traversal Shielding**: Input parsing restricts I/O strictly to defined stream paths.
- **Immutability by Default**: Null parameters are aggressively intercepted. `Map.copyOf` / unmodifiable map constraints defend against malicious cross-thread tampering.

## ⚡ Performance SLAs

- **O(1) Memory Footprint (Constant Space)**: Data streams directly through Apache Commons CSV sequentially instead of loading millions of rows entirely into RAM via `readAllLines()`.
- **Garbage Collection Optimization**: The `TaxCalculator` incorporates the Flyweight Pattern pre-compiling `BigDecimal` constants (`10.00`, `100`, `0.00`) and regex expressions, saving thousands of iterative CPU allocations.
- **Precision Assured**: All math functions utilize 16-digit precision `RoundingMode.HALF_UP` inside calculation loops, eliminating standard floating-point drift.

## 📦 Build & Test

```bash
mvn clean verify
```
Requires Java 17+. JaCoCo guarantees >= 95% C0 coverage, 90% C1 branch coverage across the repository structure.
