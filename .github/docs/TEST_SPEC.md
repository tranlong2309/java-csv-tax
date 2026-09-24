# KẾ HOẠCH VÀ KỊCH BẢN KIỂM THỬ (TEST PLAN & TEST CASES)

---

## 1. MỤC TIÊU KIỂM THỬ

* Độ bao phủ mã nguồn (Line & Branch Coverage): **>= 90%**.
* Kiểm thử toàn bộ các trường hợp tính toán chính xác và các trường hợp biên/dữ liệu rác.

---

## 2. DANH SÁCH TEST CASES CHI TIẾT

| ID Test | Tên Scenario | Input CSV | Kết quả kỳ vọng |
| :--- | :--- | :--- | :--- |
| **TC-01** | Tính toán file chuẩn (Standard Input) | `Mặt hàng,Số lượng,Đơn giá,% VAT` <br> `Áo sơ mi,2,100000,10` | Subtotal: 200,000 <br> VAT: 20,000 <br> Total: 220,000 |
| **TC-02** | Formats VAT linh hoạt | `Laptop,1,15000000,0.1` <br> `Chuột,2,200000,8%` | VAT Laptop: 1,500,000 (10%) <br> VAT Chuột: 32,000 (8%) |
| **TC-03** | Làm tròn số tiền lẻ | `Sản phẩm X,3,33333,10` | Subtotal: 99,999 <br> VAT: 10,000 (9,999.9 làm tròn) <br> Total: 109,999 |
| **TC-04** | Dòng dữ liệu thiếu/trống | `Ao,2,,10` (Thiếu đơn giá) | Đánh dấu dòng lỗi `MISSING_UNIT_PRICE`, thêm vào list Warnings |
| **TC-05** | Dữ liệu chứa CSV Injection | `=SUM(A1:A10),1,50000,10` | Output CSV sanitized: `'=SUM(A1:A10)` |
| **TC-06** | Dynamic JSON Metadata | File JSON đổi tên cột thành `Price`, `Qty` | Parse thành công theo đúng mapping |
| **TC-07** | Strict Mode vs Lenient Mode | CSV có dòng lỗi | Strict Mode: Ném `ValidationException` <br> Lenient Mode: Bỏ qua dòng lỗi, trả report có warnings |
| **TC-08** | Performance Test | File CSV 10,000 dòng | Xử lý hoàn tất dưới 200ms |

---

## 3. MÃ NGUỒN UNIT TEST MẪU (JUNIT 5 & ASSERTJ)

```java
@Test
void testStandardTaxCalculation_Success() {
    String csvData = "mat_hang,so_luong,don_gia,phan_tram_vat\n" +
                     "Ao so mi,2,100000,10\n" +
                     "Quan jean,1,200000,8";

    TaxSummaryReport report = TaxProcessor.builder()
            .process(new StringReader(csvData));

    assertThat(report.getItemResults()).hasSize(2);
    assertThat(report.getGrandSubtotal()).isEqualByComparingTo("400000.00");
    assertThat(report.getGrandTotalVat()).isEqualByComparingTo("36000.00");
    assertThat(report.getGrandTotalAmount()).isEqualByComparingTo("436000.00");
}
```
