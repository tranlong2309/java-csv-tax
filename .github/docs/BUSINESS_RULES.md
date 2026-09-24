# QUY TẮC NGHIỆP VỤ VÀ CÔNG THỨC TÍNH TOÁN (BUSINESS RULES SPEC)

---

## 1. QUY TẮC TÍNH TOÁN TÀI CHÍNH (FINANCIAL MATH RULES)

### BR-01: Độ chính xác và Làm tròn (Precision & Rounding)
* Toàn bộ phép tính số học **BẮT BUỘC** dùng `java.math.BigDecimal`.
* KHÔNG sử dụng `double` hoặc `float` trong bất kỳ phép tính tiền tệ nào.
* Quy tắc làm tròn: **`RoundingMode.HALF_UP`** (Làm tròn lên khi chữ số tiếp theo >= 5).
* Quy mô chữ số thập phân (Scale):
  * **Số lượng (`quantity`):** Scale 4 chữ số thập phân (Ví dụ: `1.5000`).
  * **Đơn giá (`unitPrice`):** Scale 4 chữ số thập phân.
  * **Phần trăm VAT (`vatRate`):** Scale 2 chữ số thập phân (Ví dụ: `10.00`).
  * **Tiền VAT (`vatAmount`):** Scale 2 chữ số thập phân (đối với VND làm tròn đến hàng đơn vị hoặc 2 chữ số thập phân theo cấu hình).
  * **Tổng tiền (`totalAmount`):** Scale 2 chữ số thập phân.

---

## 2. CÔNG THỨC TOÁN HỌC (MATHEMATICAL FORMULAS)

1. **Thành tiền trước thuế của từng mặt hàng ($Subtotal_i$):**
   $$Subtotal_i = Quantity_i \times UnitPrice_i$$

2. **Tiền thuế VAT của từng mặt hàng ($VatAmount_i$):**
   $$VatAmount_i = \text{Round}_{HALF\_UP}\left(Subtotal_i \times \frac{VatRate_i}{100}, 2\right)$$

3. **Tổng tiền sau thuế của từng mặt hàng ($TotalAmount_i$):**
   $$TotalAmount_i = Subtotal_i + VatAmount_i$$

4. **Tổng thành tiền chưa thuế toàn bộ file ($GrandSubtotal$):**
   $$GrandSubtotal = \sum_{i=1}^{N} Subtotal_i$$

5. **Tổng tiền thuế VAT toàn bộ file ($GrandTotalVAT$):**
   $$GrandTotalVAT = \sum_{i=1}^{N} VatAmount_i$$

6. **Tổng tiền thanh toán toàn bộ file ($GrandTotalAmount$):**
   $$GrandTotalAmount = GrandSubtotal + GrandTotalVAT$$

---

## 3. THUẬT TOÁN PARSE % VAT (VAT RATE PARSING ALGORITHM)

Trình xử lý VAT rate nhận chuỗi ký tự từ CSV và chuyển đổi theo các bước:
1. Strip khoảng trắng hai đầu chuỗi.
2. Nếu chuỗi kết thúc bằng ký tự `%`, loại bỏ ký tự `%` và chuyển đổi phần còn lại sang `BigDecimal`.
3. Nếu giá trị nằm trong khoảng $(0, 1)$ (ví dụ: `"0.1"` hoặc `"0.08"`), nhân với `100` để đưa về dạng phần trăm chuẩn (`10.00%` hoặc `8.00%`).
4. Nếu giá trị $>= 1$ (ví dụ: `"10"`, `"8"`), giữ nguyên giá trị làm phần trăm.
5. Nếu giá trị $< 0$ hoặc không thể parse thành số -> Đánh dấu dòng lỗi `INVALID_VAT_RATE`.

---

## 4. QUY TẮC CHUẨN HÓA TÊN CỘT (HEADER NORMALIZATION)

Để nhận diện cột thông minh khi không có file JSON metadata:
1. Chuyển toàn bộ tên cột về chữ thường (`toLowerCase()`).
2. Loại bỏ dấu tiếng Việt (ví dụ: `mặt hàng` -> `mat hang`).
3. Loại bỏ ký tự đặc biệt, ký tự khoảng trắng và dấu gạch dưới `_`.
4. Bảng ánh xạ mặc định:
   * **Item Name:** `mathang`, `tenmathang`, `itemname`, `item`, `description`, `sanpham`
   * **Quantity:** `soluong`, `quantity`, `qty`, `sl`
   * **Unit Price:** `dongia`, `unitprice`, `price`, `gia`
   * **VAT Rate:** `phantramvat`, `vatrate`, `vat`, `thuevat`, `thue`
   * **Subtotal (Optional Input):** `sotong`, `subtotal`, `thanhtien`
