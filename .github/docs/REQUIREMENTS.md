# YÊU CẦU DỰ ÁN (REQUIREMENTS SPECIFICATION)
## Shared Java CSV Tax Processing Library

---

## 1. TỔNG QUAN VÀ MỤC TIÊU DỰ ÁN

### 1.1. Bối cảnh
Công ty mẹ cần một **Thư viện Java dùng chung (Shared Java Library)** nhằm chuẩn hóa quy trình xử lý dữ liệu tài chính và tính thuế VAT từ các file dữ liệu CSV. Thư viện này sẽ được nhúng vào nhiều dự án nội bộ khác nhau (như hệ thống bán hàng, kế toán, quản lý hóa đơn) mà không cần viết lại mã nguồn tính toán.

### 1.2. Mục tiêu chính
* **Tính tái sử dụng cao:** Cung cấp API đơn giản (Fluent Interface) dễ dàng tích hợp vào bất kỳ ứng dụng Java nào (Java 11 - 21).
* **Độ chính xác tài chính tuyệt đối:** Xử lý tính toán tiền thuế, tổng thanh toán bằng `BigDecimal` loại bỏ hoàn toàn sai số dấu phẩy động (`double`/`float`).
* **Linh hoạt theo cấu hình Metadata:** Không hardcode tên cột CSV; hỗ trợ cấu hình ánh xạ linh hoạt qua JSON Metadata để hoạt động với file CSV từ nhiều nguồn khác nhau.
* **Xử lý lỗi thông minh (Fault-Tolerant):** Tiếp tục xử lý các dòng hợp lệ khi gặp dòng dữ liệu rác, tổng hợp danh sách cảnh báo chi tiết thay vì làm ngắt toàn bộ ứng dụng.

---

## 2. YÊU CẦU CHỨC NĂNG (FUNCTIONAL REQUIREMENTS - FR)

### FR-01: Đọc và Parse Dữ liệu CSV Đầu Vào
* **Mô tả:** Thư viện hỗ trợ nhận đầu vào linh hoạt từ `File`, `InputStream`, `Reader`, hoặc `String` chứa nội dung CSV.
* **Định dạng hỗ trợ:**
  * Tự động nhận diện dấu phân cách cột (phẩy `,`, chấm phẩy `;`, tab `	`).
  * Bỏ qua các dòng trống hoặc dòng comment.
  * Hỗ trợ UTF-8 có hoặc không có BOM.

### FR-02: Ánh Xạ Cấu Trúc Bằng Metadata (Dynamic Metadata Mapping)
* **Mô tả:** Ánh xạ các cột trong file CSV với các trường dữ liệu nghiệp vụ chuẩn (`itemName`, `quantity`, `unitPrice`, `vatRate`, `subtotal`).
* **Cơ chế:**
  * **Default Mapping:** Tự động nhận diện tên cột chuẩn tiếng Việt (`mat_hang`, `so_luong`, `don_gia`, `phan_tram_vat`, `so_tong`) và tiếng Anh (`item_name`, `quantity`, `unit_price`, `vat_rate`, `subtotal`).
  * **Custom Mapping:** Nhận file/chuỗi JSON Metadata do dự án tích hợp truyền vào để định nghĩa tên cột riêng.

### FR-03: Tính Toán Thuế VAT Chi Tiết Từng Mặt Hàng
* **Mô tả:** Đối với mỗi dòng sản phẩm trong CSV, tính toán chính xác:
  * **Thành tiền trước thuế (Subtotal):** Subtotal = Quantity * UnitPrice
  * **Tiền thuế VAT (VAT Amount):** VatAmount = Subtotal * (VatRate / 100) (Làm tròn theo quy tắc `HALF_UP` đến 2 chữ số thập phân).
  * **Tổng tiền sau thuế (Total Amount):** TotalAmount = Subtotal + VatAmount

### FR-04: Xử Lý Biểu Diễn Phần Trăm VAT Linh Hoạt
* **Mô tả:** Đọc và quy đổi các định dạng % VAT đầu vào khác nhau về giá trị số thập phân chuẩn:
  * Số nguyên/thập phân: `"10"`, `"8"`, `"5"`, `"0"` -> 10%, 8%, 5%, 0%.
  * Tỷ lệ thập phân: `"0.1"` -> 10%.
  * Chuỗi chứa ký tự `%`: `"10%"` -> 10%.

### FR-05: Tổng Hợp Báo Cáo Tài Chính (Tax Summary Report)
* **Mô tả:** Tính toán tổng mức toàn bộ file dữ liệu:
  * **Grand Subtotal:** Tổng tiền chưa thuế của tất cả dòng hợp lệ.
  * **Grand Total VAT:** Tổng tiền thuế VAT của tất cả dòng hợp lệ.
  * **Grand Total Amount:** Tổng số tiền thanh toán cuối cùng.
  * **Valid Row Count / Invalid Row Count:** Số lượng dòng xử lý thành công và số dòng bị lỗi.

### FR-06: Xuất Kết Quả Đa Dạng
* **Mô tả:** Hỗ trợ xuất dữ liệu đã được tính toán dưới các định dạng:
  * Trả về Đối tượng Java (DTO Java Object) `TaxSummaryReport`.
  * Xuất file CSV mới được bổ sung các cột tiền thuế (`tien_vat`, `tong_thanh_toan`).
  * Trả về chuỗi `String CSV` hoặc ghi vào `OutputStream`.

---

## 3. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - NFR)

### NFR-01: Hiệu Năng & Bộ Nhớ
* Xử lý file CSV 10,000 dòng trong dưới **200ms**.
* Sử dụng cơ chế Streaming (không load toàn bộ file lớn vào RAM cùng lúc) để đảm bảo bộ nhớ tiêu thụ không vượt quá **50MB RAM** đối với file dữ liệu lớn.

### NFR-02: Tương Thích & Độc Lập Thư Viện
* Tương thích từ **Java 11 LTS đến Java 21 LTS**.
* Mã nguồn Clean Java, hạn chế tối đa phụ thuộc thư viện bên thứ 3.

### NFR-03: Thread Safety
* Toàn bộ lớp DTO là Immutability (không thể thay đổi state).
* Lớp Service xử lý chính là **Stateless**, cho phép nhiều Thread gọi đồng thời an toàn.

### NFR-04: Chuẩn Mã Nguồn & Chất Lượng
* Mã nguồn đạt chuẩn Clean Code, áp dụng đúng các Design Pattern (Fluent Builder, Strategy, Facade).
* Độ bao phủ kiểm thử (Code Coverage) đạt tối thiểu **90%**.
