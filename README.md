# CSV Processing Platform

This is a multi-module Maven project providing a robust, domain-agnostic CSV processing core and a domain-specific extension for tax calculation.

## Architecture

The project has been refactored into a layered architecture to cleanly separate generic CSV parsing logic from domain-specific rules:

- **`csv-core`**: A generic, reusable library for reading, sanitizing, and writing CSV files. It is strictly domain-agnostic. It does not know anything about taxes, products, or other specific business logic.
- **`csv-tax-extension`**: A consumer of `csv-core` that implements domain-specific logic for processing tax invoices.

## Module 1: `csv-core` (Generic CSV Engine)

The core module provides the `CsvProcessor` facade to read CSV rows into Java objects and write objects back to CSV. It includes:

- **`CsvReaderEngine`**: Reads CSV rows based on strict or lenient column mapping.
- **`CsvWriterEngine`**: Writes CSV rows and automatically sanitizes dangerous characters (`=`, `+`, `-`, `@`, `\t`, `\r`) to prevent CSV Injection.
- **`CsvColumnMappingConfig`**: Manages flexible JSON-based metadata configurations for dynamic column mapping and dynamic delimiter selection.
- **`RecordCalculator<T>`**: An interface that consumers implement to transform raw `Map<String, String>` rows into their specific domain models.

### Usage Example (Core Library)

```java
// 1. Define configuration with dynamic metadata
CsvColumnMappingConfig config = CsvColumnMappingConfig.fromJson("{\"csvDelimiter\": \",\", \"lenientMode\": true}");

// 2. Build the processor by passing your own RecordCalculator
CsvProcessor<MyDomainModel> processor = CsvProcessor.<MyDomainModel>builder()
    .withMetadata(config)
    .withRowCalculator(new MyDomainCalculator())
    .build();

// 3. Process the CSV
List<MyDomainModel> results = processor.process(new File("input.csv"), warning -> {
    System.out.println("Warning at line " + warning.getLineNumber() + ": " + warning.getMessage());
});
```

## Module 2: `csv-tax-extension`

Please see [csv-tax-extension/README.md](csv-tax-extension/README.md) for details on how to use the Tax Calculator functionality.

## Refactoring Summary (Changelog)

During the migration from the monolithic `java-csv-tax` structure to the multi-module `csv-processing-parent` structure, the following changes were made:

### Renamed/Moved Files (to `csv-core`)
- `com.company.taxlibrary.CsvProcessor` -> `com.company.csvengine.CsvProcessor` (Made Generic)
- `com.company.taxlibrary.parser.CsvReaderEngine` -> `com.company.csvengine.parser.CsvReaderEngine`
- `com.company.taxlibrary.parser.CsvWriterEngine` -> `com.company.csvengine.parser.CsvWriterEngine`
- `com.company.taxlibrary.config.CsvColumnMappingConfig` -> `com.company.csvengine.config.CsvColumnMappingConfig`
- `com.company.taxlibrary.util.CsvSanitizer` -> `com.company.csvengine.util.CsvSanitizer`
- `com.company.taxlibrary.util.HeaderNormalizer` -> `com.company.csvengine.util.HeaderNormalizer`
- `com.company.taxlibrary.exception.*` -> `com.company.csvengine.exception.*`
- `com.company.taxlibrary.model.ValidationWarning` -> `com.company.csvengine.model.ValidationWarning`

### Created Files (in `csv-core`)
- `com.company.csvengine.calc.RecordCalculator<T>`: New interface to decouple domain logic from CSV reading.

### Kept in `csv-tax-extension` (Domain Specific)
- `com.company.taxlibrary.TaxProcessor`: Facade for tax processing, acts as a wrapper over `CsvProcessor<TaxItemOutput>`.
- `com.company.taxlibrary.builder.TaxProcessorBuilder`: Fluent builder for the `TaxProcessor`.
- `com.company.taxlibrary.service.TaxCalculator`: Implements `RecordCalculator<TaxItemOutput>`.
- `com.company.taxlibrary.model.*`: `TaxSummaryReport`, `TaxItemOutput`.
- `com.company.taxlibrary.util.TaxHeaderHelper`: Contains static default tax headers and VAT parsers.

### Security Enhancements
- `SecuritySanitizerTest.java` was comprehensively rewritten in `csv-tax-extension` to cover CSV injection mitigation (`=`, `+`, `-`, `@`, `\t`, `\r`), NullPointer exceptions guard, and strict mode validation.
