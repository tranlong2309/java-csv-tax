# TÀI LIỆU AN NINH VÀ BẢO MẬT (SECURITY & COMPLIANCE SPECIFICATION)

---

## 1. PHÒNG CHỐNG CSV FORMULA INJECTION (EXCEL MACRO INJECTION)

### 1.1. Bối cảnh rủi ro
Khi dữ liệu xuất ra file CSV được mở bằng Microsoft Excel hoặc Google Sheets, nếu cột tên mặt hàng chứa các ký tự khởi đầu như `=`, `+`, `-`, `@`, `\t`, `\r`, Excel sẽ tự động hiểu đó là công thức (Formula) và có thể thực thi mã độc hoặc chiếm quyền điều khiển.

### 1.2. Biện pháp xử lý (`CsvSanitizer.java`)
Mọi chuỗi dữ liệu văn bản trước khi ghi ra file CSV kết quả **BẮT BUỘC** phải chạy qua hàm sanitize:
```java
public class CsvSanitizer {
    private static final char[] DANGEROUS_PREFIXES = {'=', '+', '-', '@', '\t', '\r'};

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        char firstChar = input.charAt(0);
        for (char prefix : DANGEROUS_PREFIXES) {
            if (firstChar == prefix) {
                // Thêm dấu nháy đơn ' ở đầu để Excel hiểu là văn bản thuần (Text)
                return "'" + input;
            }
        }
        return input;
    }
}
```

---

## 2. PHÒNG CHỐNG TẤN CÔNG TỪ CHỐI DỊCH VỤ (DOS & MEMORY EXHAUSTION)

1. **Giới hạn kích thước file CSV:** Mặc định từ chối xử lý các file CSV có dung lượng vượt quá **50MB** (có thể cấu hình lại qua Builder).
2. **Giới hạn số dòng xử lý:** Mặc định cảnh báo và ngắt tiến trình nếu số dòng vượt quá **100,000 dòng**.
3. **Sử dụng Streaming Parser:** Không đọc toàn bộ file vào bộ nhớ RAM (`ReadAllLines`), sử dụng `BufferedReader` kết hợp `Iterator` để xử lý từng dòng một.

---

## 3. AN TOÀN THỰC THI VÀ ĐA LUỒNG (THREAD SAFETY)

1. **Immutability:** Các lớp lưu giữ trạng thái kết quả (`TaxItemResult`, `TaxSummaryReport`) là `final` và các tập hợp bên trong dùng `Collections.unmodifiableList()`.
2. **Stateless Services:** Các lớp `TaxCalculationEngine`, `ApacheCommonsCsvParser` không giữ state cấp class, hoàn toàn an toàn khi sử dụng trong môi trường Multi-threaded (như Spring Boot Web Application).
