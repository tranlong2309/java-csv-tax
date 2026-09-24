# Coding Rules & Guidelines (Quy tắc lập trình)

Dưới đây là các quy tắc lập trình (Coding Rules) bắt buộc áp dụng cho toàn bộ dự án nhằm đảm bảo Clean Code, dễ bảo trì và đạt hiệu suất cao.

## 1. Định dạng Code (Formatting & Clean Code)
- **Xuống dòng sạch đẹp (Clean Spacing):** Code phải có khoảng trắng và xuống dòng rõ ràng giữa các khối logic, các phương thức (methods) và các câu lệnh khác nhau để dễ đọc. Mỗi block logic nên cách nhau 1 dòng trống.
- **Dấu ngoặc nhọn bắt buộc (Braces Requirement):** Tất cả các khối lệnh điều kiện và vòng lặp (`if`, `else`, `for`, `while`) ĐỀU PHẢI có dấu ngoặc nhọn `{}`, kể cả khi khối lệnh chỉ có duy nhất một dòng.
- **Xuống hàng đầy đủ:** Dấu ngoặc nhọn mở `{` được đặt ở cuối dòng, nhưng nội dung bên trong bắt buộc phải xuống hàng và thụt lề chuẩn (indentation). KHÔNG viết gộp nhiều lệnh trên cùng một dòng (vd: `if (a) { return b; }`).

## 2. Comment & Tài liệu (Documentation)
- **Comment đầy đủ (Full Documentation):**
  - Mọi lớp (class) và phương thức (method) công khai (public) phải có Javadoc giải thích rõ mục đích, tham số (`@param`) và giá trị trả về (`@return`).
  - Các đoạn logic phức tạp trong phương thức (core business logic) phải có comment nội tuyến (inline comment) giải thích *tại sao* (why) lại làm như vậy.

## 3. Tối ưu Hiệu suất (Performance Optimization)
- **Hạn chế vòng lặp lồng nhau (Avoid nested loops):** Cần tối ưu hóa độ phức tạp thuật toán. Tránh gọi các hàm tốn kém lặp đi lặp lại bên trong vòng lặp lồng nhau (vd: chuẩn hóa chuỗi nhiều lần O(N*M), thay vào đó hãy lưu biến tạm O(N)).
- **Tái sử dụng đối tượng (Object Reuse):** Hạn chế khởi tạo các đối tượng (như `String`, `Map`, `List`) bên trong vòng lặp nếu chúng có thể được cấp phát ở ngoài (ví dụ: Compile Regex một lần ở biến tĩnh, chuẩn hóa List cấu hình một lần).
- **Tránh tính toán lại thừa thãi:** Lưu kết quả của các biểu thức thường xuyên lặp lại vào biến trung gian `final`.

## 4. Các Quy tắc Bổ sung Tốt nhất cho Dự án (Best Practices)
- **Tính Bất biến (Immutability):** Khuyến khích sử dụng từ khóa `final` cho các biến, tham số phương thức và đối tượng khi chúng không cần thay đổi giá trị. Điều này ngăn ngừa vô tình gán nhầm và tối ưu cho Thread-Safe.
- **Early Return (Rẽ nhánh sớm):** Xử lý các luồng lỗi hoặc điều kiện biên ngay ở đầu phương thức và `return` hoặc `throw` sớm. Điều này giúp giảm thiểu độ lồng sâu của các khối `if-else` (deep nesting).
- **Fail-Fast (Phát hiện lỗi sớm):** Validate các tham số đầu vào (ví dụ: dùng `Objects.requireNonNull`) ngay lập tức để chương trình báo lỗi nhanh chóng nếu dữ liệu không hợp lệ.
- **Kiểm soát Ngoại lệ (Exception Handling):** Bắt chính xác loại ngoại lệ cụ thể, tránh bắt `Exception` chung chung. Các khối catch phải log hoặc throw ra thông điệp có ý nghĩa.
