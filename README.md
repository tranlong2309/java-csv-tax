# Shared CSV Tax Library

## 📖 Giới thiệu (Introduction)

**Shared CSV Tax Library** là một thư viện Java 17 hiệu suất cao, dựa trên cơ chế streaming (luồng) để phân tích cú pháp (parsing) các bản ghi thuế từ tệp CSV. Thư viện tính toán chính xác các khoản tài chính VAT, ánh xạ (map) các cột một cách linh hoạt thông qua siêu dữ liệu (metadata) JSON và tạo các báo cáo thuế chi tiết một cách an toàn mà không có nguy cơ gây cạn kiệt bộ nhớ.

Thư viện được thiết kế đặc biệt cho các hệ thống cần xử lý lượng lớn dữ liệu hóa đơn, chứng từ thuế một cách an toàn, nhanh chóng và chính xác tuyệt đối.

## 🏗 Kiến trúc thư viện (Architecture)

Thư viện tuân thủ nghiêm ngặt các nguyên tắc Clean Code và SOLID:
- **`TaxProcessor` (Facade)**: Điểm truy cập công khai (public entry point) an toàn luồng (thread-safe) dành cho client.
- **Fluent Builder**: `TaxProcessorBuilder` hỗ trợ cấu hình động trước khi khởi tạo đối tượng.
- **`CsvReaderEngine` & `CsvWriterEngine` (Encapsulated)**: Các parser nội bộ sử dụng Apache Commons CSV để thực hiện streaming I/O mà không làm tăng dung lượng bộ nhớ.
- **Mô hình Domain Bất biến (Immutable Domain Models)**: Tất cả các báo cáo (`TaxSummaryReport`, `TaxItemOutput`) và cấu hình (`MetadataConfig`) được bảo vệ nghiêm ngặt bằng cách sử dụng `java.lang.Record` và `Collections.unmodifiableMap()`, ngăn chặn việc sửa đổi từ bên ngoài.

## 🛡 Tính năng Bảo mật (Security Features)

- **Ngăn chặn Injection Công thức (Formula Injection Defense)**: Tất cả các ô CSV được tạo ra bắt đầu bằng các ký tự nguy hiểm của Excel (`=`, `+`, `-`, `@`, `\t`, `\r`) đều được vô hiệu hóa an toàn bằng cách thêm một dấu nháy đơn (`'`) vào phía trước.
- **Chống Path Traversal**: Việc phân tích đầu vào (Input parsing) giới hạn I/O nghiêm ngặt trong các đường dẫn luồng đã được xác định.
- **Bất biến theo mặc định (Immutability by Default)**: Các tham số Null bị chặn đứng ngay lập tức. Các ràng buộc `Map.copyOf` / unmodifiable map giúp chống lại sự can thiệp nguy hiểm xuyên luồng (cross-thread).

## ⚡ Hiệu suất (Performance SLAs)

- **O(1) Bộ nhớ (Constant Space)**: Dữ liệu được luân chuyển tuần tự qua Apache Commons CSV thay vì tải toàn bộ hàng triệu dòng vào RAM qua `readAllLines()`.
- **Tối ưu hóa Garbage Collection**: `TaxCalculator` kết hợp mẫu thiết kế Flyweight Pattern để biên dịch trước các hằng số `BigDecimal` (`10.00`, `100`, `0.00`) và các biểu thức regex, giúp tiết kiệm hàng ngàn phân bổ CPU lặp đi lặp lại.
- **Đảm bảo Độ chính xác tuyệt đối**: Tất cả các hàm toán học sử dụng `BigDecimal` với độ chính xác 16 chữ số và `RoundingMode.HALF_UP` bên trong các vòng lặp tính toán, loại bỏ hoàn toàn sai số của floating-point.

## 🛠 Hướng dẫn Cài đặt (Installation Guide)

Để sử dụng thư viện, bạn cần thêm dependency sau vào file `pom.xml` (Maven):

```xml
<dependency>
    <groupId>com.company</groupId>
    <artifactId>shared-csv-tax-library</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 📚 Hướng dẫn Sử dụng Chi tiết (Detailed Usage Guide)

### 1. Khởi tạo `TaxProcessor`

`TaxProcessor` hỗ trợ Fluent API, cho phép linh hoạt cấu hình tham số đầu vào. Thư viện hỗ trợ đọc từ nhiều nguồn: `File`, `Path`, `InputStream`, `String`, hoặc `Reader`. Tất cả các luồng tài nguyên đều sử dụng `try-with-resources` một cách tự nhiên, đảm bảo không rò rỉ bộ nhớ hoặc khóa file treo.

```java
import com.company.taxlibrary.TaxProcessor;

// Khởi tạo processor với cấu hình cơ bản (An toàn luồng & Bất biến)
TaxProcessor processor = TaxProcessor.builder()
        .enableLenientMode(true) // Tiếp tục xử lý ngay cả khi một dòng bị lỗi
        .withDelimiter(";")      // Ký tự phân cách (VD: ;)
        .build();
```

### 2. Xử lý tệp CSV và Lấy Báo cáo

Sau khi khởi tạo `TaxProcessor`, bạn có thể truyền đầu vào để phân tích và tính toán thuế.

```java
import com.company.taxlibrary.model.TaxSummaryReport;
import java.io.File;
import java.nio.file.Path;

public class TaxExecutionExample {
    public static void main(String[] args) {
        TaxProcessor processor = TaxProcessor.builder().build();
        
        // Cách 1: Sử dụng File
        TaxSummaryReport report1 = processor.process(new File("tax-input.csv"));
        
        // Cách 2: Sử dụng Path
        TaxSummaryReport report2 = processor.process(Path.of("data.csv"));
        
        // Cách 3: Sử dụng String trực tiếp
        TaxSummaryReport report3 = processor.process("item,qty,price,vat\nLaptop,1,1000,10%");
        
        // Xử lý báo cáo đầu ra
        System.out.println("Tổng cộng (Chưa VAT): $" + report1.getGrandSubtotal());
        System.out.println("Tổng VAT: $" + report1.getGrandTotalVat());
        System.out.println("Tổng cộng (Đã bao gồm VAT): $" + report1.getGrandTotalAmount());
        
        // In chi tiết từng mặt hàng
        report1.getItemResults().forEach(item -> {
            System.out.println("Mặt hàng: " + item.getItemName() + " | Tổng tiền: $" + item.getTotalAmount());
        });
    }
}
```

### 3. Cấu hình Ánh xạ Cột qua JSON Metadata (JSON Metadata Setup)

Nếu tiêu đề (headers) trong CSV của bạn không theo tiêu chuẩn mặc định, bạn có thể tự động ánh xạ chúng thông qua file cấu hình JSON Metadata.

Ví dụ tạo file **`metadata.json`**:
```json
{
  "csvDelimiter": ",",
  "charset": "UTF-8",
  "lenientMode": true,
  "columnMapping": {
    "itemNameHeader": ["Product", "Tên Hàng", "Sản phẩm"],
    "quantityHeader": ["Qty", "Số Lượng"],
    "unitPriceHeader": ["Price", "Đơn Giá"],
    "vatRateHeader": ["Tax", "VAT", "Thuế"]
  }
}
```

Sử dụng metadata này khi khởi tạo processor:
```java
TaxProcessor processor = TaxProcessor.builder()
        .withMetadata(new File("metadata.json"))
        .build();
```
*Lưu ý: Quá trình chuẩn hóa header tự động sẽ loại bỏ UTF-8 BOM, cắt bỏ khoảng trắng (trim), chuẩn hóa chữ hoa/chữ thường và làm sạch an toàn các dấu tiếng Việt.*

## 📦 Build & Chạy Test

Để build mã nguồn và chạy toàn bộ các bài kiểm thử, sử dụng lệnh Maven sau:

```bash
mvn clean verify
```
*Yêu cầu Java 17+. Thư viện sử dụng JaCoCo đảm bảo độ bao phủ mã (coverage) >= 95% C0 và 90% C1 (branch coverage) trên toàn bộ dự án.*

## 📑 Tài liệu API (JavaDocs)

Bạn có thể tự động tạo và xem tài liệu API (JavaDocs) chi tiết cho toàn bộ thư viện bằng cách sử dụng file script đi kèm.

```bash
# Cấp quyền thực thi cho script (chỉ cần làm 1 lần)
chmod +x generate_javadoc.sh

# Chạy script để tạo và tự động mở JavaDocs trên trình duyệt
./generate_javadoc.sh
```
*Tài liệu sau khi tạo sẽ được lưu tại `target/site/apidocs/index.html`.*
