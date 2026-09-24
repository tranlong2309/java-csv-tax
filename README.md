# Thư viện Shared CSV Tax

Thư viện Java 11+ dùng chung để xử lý CSV theo streaming, tính VAT, ánh xạ cột bằng metadata, báo cáo lỗi và xuất CSV an toàn.

## Bắt đầu nhanh

### Maven

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>shared-csv-tax-library</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation 'com.company:shared-csv-tax-library:1.0.0-SNAPSHOT'
}
```

Cài đặt bản snapshot vào Maven local bằng `mvn install`, hoặc publish artifact vào Maven repository nội bộ. Thư viện target Java 11 và tương thích Java 11 đến Java 21 LTS.

## Ví dụ trong năm phút

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class TaxExample {
    public static void main(String[] args) {
        File metadataFile = new File("metadata.json");
        File inputCsv = new File("tax-input.csv");

        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata(metadataFile)
                .process(inputCsv);

        TaxProcessor processor = TaxProcessor.builder().build();
        processor.process(Path.of("tax-input.csv"));
        processor.process(new StringReader(
                "mat_hang,so_luong,don_gia,phan_tram_vat\nItem,1,100,10\n"));
        processor.process(new ByteArrayInputStream(
                "mat_hang,so_luong,don_gia,phan_tram_vat\nItem,1,100,10\n"
                        .getBytes(StandardCharsets.UTF_8)));

        System.out.println("Tổng trước thuế: " + report.getGrandSubtotal());
        System.out.println("Tổng VAT: " + report.getGrandTotalVat());
        System.out.println("Tổng thanh toán: " + report.getGrandTotalAmount());
        System.out.println("Số cảnh báo: " + report.getValidationWarnings().size());
    }
}
```

Nếu dùng tên cột mặc định, metadata là không bắt buộc: gọi `TaxProcessor.builder().process(new File("tax-input.csv"))`.

## Định dạng đầu vào

Facade hỗ trợ `File`, `Path`, `InputStream`, `Reader` và chuỗi CSV:

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

public final class StringInputExample {
    public static void main(String[] args) {
        TaxSummaryReport report = TaxProcessor.builder().process(
                "mat_hang,so_luong,don_gia,phan_tram_vat\nNotebook,2,100000,10%\n");
        System.out.println(report.getGrandTotalAmount());
    }
}
```

Các overload nhận `File`, `Path` và `InputStream` quản lý resource bằng try-with-resources. Overload nhận `Reader` không đóng reader do caller sở hữu resource đó.

## Cột CSV và Metadata

Các alias mặc định được hỗ trợ:

| Trường | Alias tiếng Việt và tiếng Anh |
| --- | --- |
| Tên mặt hàng | `mat_hang`, `ten_mat_hang`, `item_name`, `item`, `description`, `sanpham` |
| Số lượng | `so_luong`, `quantity`, `qty`, `sl` |
| Đơn giá | `don_gia`, `unit_price`, `price`, `gia` |
| Thuế VAT | `phan_tram_vat`, `vat_rate`, `vat`, `thuevat`, `thue` |
| Thành tiền | `so_tong`, `subtotal`, `thanhtien` (không bắt buộc) |

Việc so khớp bỏ qua khoảng trắng đầu/cuối, chữ hoa/thường, dấu tiếng Việt, dấu câu, dấu gạch dưới và UTF-8 BOM. Cột thành tiền là tùy chọn vì thư viện tự tính từ số lượng và đơn giá.

### JSON Metadata

```json
{
  "csvDelimiter": ";",
  "charset": "UTF-8",
  "lenientMode": true,
  "columnMapping": {
    "itemNameHeader": ["Product", "Description"],
    "quantityHeader": ["Qty"],
    "unitPriceHeader": ["Price"],
    "vatRateHeader": ["Tax Rate"],
    "subtotalHeader": ["Subtotal"]
  }
}
```

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

import java.io.File;

public final class MetadataFileExample {
    public static void main(String[] args) {
        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata(new File("metadata.json"))
                .withDelimiter(";")
                .process(new File("semicolon-input.csv"));
        System.out.println(report.getGrandTotalAmount());
    }
}
```

Metadata cũng có thể truyền bằng chuỗi JSON hoặc `Path`:

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

public final class MetadataStringExample {
    public static void main(String[] args) {
        TaxSummaryReport report = TaxProcessor.builder()
                .withMetadata("{\"columnMapping\":{\"itemNameHeader\":[\"Product\"]}}")
                .process("Product,so_luong,don_gia,phan_tram_vat\nItem,1,100,10\n");
        System.out.println(report.getGrandTotalAmount());
    }
}
```

## VAT và quy tắc tài chính

Mọi phép tính tài chính đều sử dụng `BigDecimal`. Có thể nhập VAT dưới các dạng `10` (10%), `0.1` (10%) hoặc `10%` (10%).

```text
subtotal  = quantity * unitPrice
vatAmount = subtotal * (vatRate / 100)
total     = subtotal + vatAmount
```

Tiền VAT và tổng tiền dùng scale 2 với `RoundingMode.HALF_UP`. Số lượng và đơn giá giữ tối đa bốn chữ số thập phân trong kết quả chi tiết.

## Báo cáo và CSV bổ sung

`TaxSummaryReport` là DTO immutable, cung cấp tổng trước thuế, tổng VAT, tổng thanh toán, danh sách kết quả từng dòng, cảnh báo validation và thời gian xử lý thông qua getter.

```java
import com.company.taxlibrary.TaxProcessor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CsvExportExample {
    public static void main(String[] args) throws Exception {
        String outputCsv = TaxProcessor.builder()
                .withMetadata(new File("metadata.json"))
                .processToCsv(new File("tax-input.csv"));
        Files.write(Path.of("tax-output.csv"), outputCsv.getBytes(StandardCharsets.UTF_8));
    }
}
```

CSV đầu ra bổ sung hai cột `tien_vat` và `tong_thanh_toan`. Mọi trường văn bản đều được sanitize trước khi ghi.

## Xử lý lỗi

Lenient mode được bật mặc định. Dòng dữ liệu lỗi sẽ bị bỏ qua khỏi tổng tiền và được thêm vào `getValidationWarnings()`:

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.model.TaxSummaryReport;

public final class LenientModeExample {
    public static void main(String[] args) {
        TaxSummaryReport report = TaxProcessor.builder()
                .enableLenientMode(true)
                .process("mat_hang,so_luong,don_gia,phan_tram_vat\nBroken,not-a-number,100,10\n");
        report.getValidationWarnings().forEach(warning ->
                System.err.println(warning.getLineNumber() + ": " + warning.getMessage()));
    }
}
```

Strict mode dừng ngay khi gặp lỗi:

```java
import com.company.taxlibrary.TaxProcessor;
import com.company.taxlibrary.exception.InvalidCsvFormatException;

import java.io.File;

public final class StrictModeExample {
    public static void main(String[] args) {
        try {
            TaxProcessor.builder().enableLenientMode(false).process(new File("tax-input.csv"));
        } catch (InvalidCsvFormatException exception) {
            System.err.println("Không thể xử lý CSV: " + exception.getMessage());
        }
    }
}
```

Metadata không hợp lệ hoặc không đọc được sẽ phát sinh `MetadataConfigException`. Các tham số API là `null` sẽ bị từ chối rõ ràng.

## Bảo mật và hiệu năng

- Giá trị bắt đầu bằng `=`, `+`, `-`, `@`, tab hoặc carriage return được thêm dấu `'` để ngăn CSV Formula Injection khi mở bằng Excel/Google Sheets.
- Apache Commons CSV xử lý theo streaming, không đọc toàn bộ file bằng `readAllLines`.
- DTO và report immutable, an toàn khi chia sẻ giữa nhiều thread.
- `TaxProcessor` có cấu hình immutable và có thể được tái sử dụng đồng thời.
- Với file upload không tin cậy, ứng dụng tích hợp nên áp dụng giới hạn kích thước file và số dòng.

## Build và kiểm thử

```bash
mvn clean test
mvn clean verify
```

Build target Java 11, bắt buộc UTF-8, chạy JUnit 5 và tạo báo cáo JaCoCo tại `target/site/jacoco/`. Lệnh `mvn verify` kiểm tra tối thiểu 95% line coverage và 90% branch coverage.

## Các package công khai

- `com.company.taxlibrary`: facade `TaxProcessor`
- `com.company.taxlibrary.builder`: fluent builder
- `com.company.taxlibrary.config`: cấu hình metadata
- `com.company.taxlibrary.model`: report và DTO immutable
- `com.company.taxlibrary.exception`: exception dành cho client

Các parser và writer là thành phần hỗ trợ nội bộ. Ứng dụng tích hợp nên sử dụng `TaxProcessor` cùng các API model/configuration.
