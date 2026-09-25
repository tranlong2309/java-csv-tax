# CSV Processing Core (Multi-Module)

This project provides a robust, memory-efficient, and domain-agnostic CSV processing engine. It is designed to handle high-volume CSV I/O securely, including protection against CSV Formula Injection.

## Modules

### 1. `csv-core`
A purely generic library (`com.company.csvengine`) for parsing, mapping, and generating CSV files.
It is entirely decoupled from any specific business logic.
- Dynamically maps columns using JSON metadata (`CsvColumnMappingConfig`).
- Protects against Formula Injection (`CsvSanitizer`).
- Provides `CsvProcessor<T>` and `RecordCalculator<T>` to stream rows and convert them to any domain object.

### 2. `csv-tax-extension`
A tax-specific implementation (`com.company.taxlibrary`) built on top of `csv-core`.
It calculates VAT and produces `TaxSummaryReport` using the generic core APIs.
