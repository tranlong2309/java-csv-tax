# Quy tắc coding và kiểm thử Java

File này là baseline cho thư viện `shared-csv-tax-library`.

## Rules

1. Dùng Java 11 LTS làm baseline; không dùng preview feature. Java 17+ chỉ được hỗ trợ sau khi cập nhật `pom.xml` và tài liệu. Spring Boot không cần thiết cho thư viện này và không được thêm nếu không có yêu cầu cụ thể.
2. Dùng `PascalCase` cho class, `camelCase` cho method/variable và `UPPER_SNAKE_CASE` cho constant.
3. Dùng exception cụ thể theo ngữ cảnh. Không catch hoặc throw `Exception`/`RuntimeException` chung nếu có thể dùng `IllegalArgumentException`, `NumberFormatException`, `InvalidCsvFormatException`, `MetadataConfigException` hoặc `TaxProcessingException`.
4. Ưu tiên constructor và giữ dependency/state trong field `private final` khi class có state.
5. Không thêm logging framework cho thư viện nếu chưa có yêu cầu; tuyệt đối không ghi secret hoặc dữ liệu cá nhân vào log.
6. Giữ method tập trung vào một trách nhiệm; tránh abstraction không cần thiết.
7. Validate dữ liệu tại boundary trước khi chuyển vào domain logic.
8. Document public class/method khi hành vi, ownership resource, thread-safety hoặc lỗi có thể không tự suy ra.
9. Ưu tiên `import` cho type dùng nhiều lần. Không dùng tên fully-qualified dài như `java.math.BigDecimal` trong thân code khi có thể thêm import.
10. Với tiền và tỷ lệ, bắt buộc dùng `BigDecimal`; không dùng `double` hoặc `float`. Dùng `RoundingMode.HALF_UP` và scale theo business rules.
11. DTO phải immutable; collection nhận từ caller phải defensive-copy và expose unmodifiable view.
12. CSV phải được xử lý streaming; mọi text ghi ra CSV phải chạy qua sanitizer chống formula injection.
13. Viết JUnit 5 + AssertJ cho happy path, boundary case, null/invalid input, security case và error mode.
14. Sau khi chỉnh sửa chạy `mvn clean test`; trước khi merge chạy `mvn clean verify` để kiểm tra JaCoCo.

## Ví dụ import

Đúng:

```java
import java.math.BigDecimal;

private BigDecimal quantity() {
	return new BigDecimal(rawQuantity);
}
```

Không dùng:

```java
private java.math.BigDecimal quantity() {
	return new java.math.BigDecimal(rawQuantity);
}
```

## Ví dụ exception

Đúng:

```java
throw new InvalidCsvFormatException("CSV row is malformed", cause);
```

Không dùng:

```java
throw new RuntimeException("Something went wrong");
```
