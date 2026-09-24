# TÀI LIỆU THIẾT KẾ KỸ THUẬT (TECHNICAL ARCHITECTURE SPECIFICATION)
## Shared Java CSV Tax Processing Library

---

## 1. MÔ HÌNH KIẾN TRÚC & PACKAGE STRUCTURE

Thư viện được thiết kế theo nguyên lý **Clean Architecture**, phân chia trách nhiệm rõ ràng giữa Parsing, Validation, Business Calculation và Result Formatting.

```
com.company.taxlibrary
├── TaxProcessor.java                  # Main Facade / Entrypoint
├── builder
│   └── TaxProcessorBuilder.java       # Fluent API Builder
├── config
│   ├── MetadataConfig.java            # Class cấu hình Metadata
│   ├── ColumnMapping.java             # Struct ánh xạ tên cột
│   └── MetadataLoader.java            # Loader đọc file Metadata JSON
├── exception
│   ├── TaxProcessingException.java    # Runtime Exception chung
│   ├── InvalidCsvFormatException.java # Lỗi cấu trúc CSV
│   └── MetadataConfigException.java   # Lỗi cấu hình Metadata
├── model
│   ├── TaxItemInput.java              # Raw Record từ CSV
│   ├── TaxItemResult.java             # Calculated Record
│   ├── TaxSummaryReport.java          # Aggregated Result DTO
│   └── ValidationWarning.java         # Object lưu cảnh báo dòng lỗi
├── parser
│   ├── CsvParserEngine.java           # Interface Parser
│   └── ApacheCommonsCsvParser.java    # Implementation dùng Apache Commons CSV
├── service
│   ├── TaxCalculationEngine.java      # Pure Business Math Engine
│   └── CsvExportService.java          # Service tạo CSV đầu ra
└── util
    ├── NumberUtils.java               # Utilities convert BigDecimal/VAT
    └── CsvSanitizer.java              # Prevention CSV Injection
```

---

## 2. ĐỊNH NGHĨA LỚP DỮ LIỆU CHÍNH (CORE DATA MODELS)

### 2.1. `MetadataConfig.java`
Lớp lưu trữ cấu hình ánh xạ cột:
```java
public class MetadataConfig {
    private String csvDelimiter = ",";
    private String charset = "UTF-8";
    private ColumnMapping columnMapping = ColumnMapping.defaultMapping();
    private boolean lenientMode = true; // Bỏ qua dòng lỗi thay vì dừng tiến trình
}
```

### 2.2. `TaxItemResult.java`
Dữ liệu tính toán chi tiết từng dòng:
```java
public class TaxItemResult {
    private final int lineNumber;
    private final String itemName;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal vatRate;     // Ví dụ: 10.00 (%)
    private final BigDecimal subtotal;    // quantity * unitPrice
    private final BigDecimal vatAmount;   // subtotal * vatRate / 100
    private final BigDecimal totalAmount; // subtotal + vatAmount
    private final boolean valid;
    private final List<String> warnings;
}
```

### 2.3. `TaxSummaryReport.java`
Báo cáo tổng hợp:
```java
public class TaxSummaryReport {
    private final BigDecimal grandSubtotal;
    private final BigDecimal grandTotalVat;
    private final BigDecimal grandTotalAmount;
    private final List<TaxItemResult> itemResults;
    private final List<ValidationWarning> validationWarnings;
    private final long processingTimeMs;
}
```

---

## 3. THIẾT KẾ CÁC DESIGN PATTERNS

### 3.1. Facade Pattern (`TaxProcessor`)
Cung cấp một điểm truy cập duy nhất cho người dùng thư viện:
```java
public class TaxProcessor {
    public static TaxProcessorBuilder builder() {
        return new TaxProcessorBuilder();
    }
}
```

### 3.2. Fluent Builder Pattern (`TaxProcessorBuilder`)
Cho phép khởi tạo quy trình xử lý mượt mà:
```java
TaxSummaryReport report = TaxProcessor.builder()
    .withMetadata(new File("metadata.json"))
    .withDelimiter(",")
    .enableLenientMode(true)
    .process(new File("input.csv"));
```

### 3.3. Strategy Pattern (`CsvParserEngine`)
Cho phép thay đổi thư viện Parse CSV bên dưới mà không ảnh hưởng đến logic tính toán tài chính.
