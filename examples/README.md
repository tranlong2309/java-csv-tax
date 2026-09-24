# Bộ file test thủ công

Thư mục này chứa dữ liệu mẫu để kiểm tra thư viện mà không cần dùng đường dẫn tuyệt đối.

## 1. Chạy test tự động

Từ thư mục gốc project:

```bash
mvn clean test
```

Chạy riêng các nhóm test:

```bash
mvn -Dtest=TaxCalculatorTest test
mvn -Dtest=HeaderNormalizerTest test
mvn -Dtest=CsvSecurityTest test
mvn -Dtest=TaxProcessorIntegrationTest test
```

## 2. Chạy demo end-to-end

Biên dịch project trước:

```bash
mvn clean package -DskipTests
```

Lấy classpath dependency của Maven:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

Biên dịch và chạy demo:

```bash
mkdir -p target/example-classes
javac -cp "target/classes:$(cat target/classpath.txt)" \
  -d target/example-classes examples/TaxProcessorDemo.java
java -cp "target/example-classes:target/classes:$(cat target/classpath.txt)" \
  TaxProcessorDemo
```

Trên Windows, thay dấu `:` trong classpath bằng dấu `;`.

Demo sẽ:

- Đọc `examples/custom-input.csv` bằng `examples/metadata-custom.json`.
- Tính subtotal, VAT và tổng thanh toán.
- Đọc `examples/tax-input.csv` bằng alias mặc định.
- Ghi kết quả ra `examples/tax-output.csv`.
- Kiểm tra công thức CSV Injection trong dòng bắt đầu bằng `=`, kết quả phải có dấu `'` phía trước.

## 3. Kiểm tra lenient mode

Chạy nhanh bằng JShell sau khi build:

```bash
jshell --class-path "target/classes:$(cat target/classpath.txt)" <<'EOF'
import com.company.taxlibrary.TaxProcessor;
import java.nio.file.Path;

var report = TaxProcessor.builder()
    .enableLenientMode(true)
    .process(Path.of("examples/tax-invalid.csv"));
System.out.println("Warnings: " + report.getValidationWarnings().size());
System.out.println("Valid rows: " + report.getItemResults().size());
EOF
```

Kết quả mong đợi: các dòng lỗi được đưa vào `ValidationWarning`, dòng hợp lệ vẫn được tính.

## 4. Kiểm tra strict mode

```bash
jshell --class-path "target/classes:$(cat target/classpath.txt)" <<'EOF'
import com.company.taxlibrary.TaxProcessor;
import java.nio.file.Path;

try {
    TaxProcessor.builder()
        .enableLenientMode(false)
        .process(Path.of("examples/tax-invalid.csv"));
} catch (RuntimeException exception) {
    System.out.println(exception.getClass().getSimpleName());
    System.out.println(exception.getMessage());
}
EOF
```

Kết quả mong đợi: thư viện dừng tại dữ liệu lỗi và phát sinh `InvalidCsvFormatException`.

## Các file mẫu

- `tax-input.csv`: dữ liệu chuẩn, VAT dạng `10`, `0.1`, `8%`, có payload formula injection.
- `tax-invalid.csv`: số lượng không hợp lệ, thiếu đơn giá và VAT âm.
- `metadata-custom.json`: mapping tên cột custom và delimiter `;`.
- `custom-input.csv`: dữ liệu tương ứng với metadata custom.
- `TaxProcessorDemo.java`: chương trình Java chạy end-to-end.
