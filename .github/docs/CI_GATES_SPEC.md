# Continuous Integration (CI) Quality Gates Specification

Tài liệu này định nghĩa hệ thống **CI Quality Gates** (Cổng kiểm soát chất lượng tự động) dành cho dự án **Shared Java CSV Tax Processing Library**. Khi mã nguồn vượt qua 100% các CI Gates này trên GitHub Actions / GitLab CI, dự án đủ điều kiện bàn giao cho công ty mẹ và khách hàng với đánh giá cao nhất (**6 giờ công**).

---

## 🚦 Danh mục 6 Cổng Kiểm Soát Chất Lượng (CI Gates)

### Gate 1: Build & Compilation Strict Gate
* **Mục tiêu:** Đảm bảo mã nguồn biên dịch sạch sẽ trên Java 17+ LTS, không có cảnh báo nghiêm trọng.
* **Lệnh thực thi:** `mvn clean compile -DcompilerArgument="-Xlint:all"`
* **Tiêu chí Đạt (Pass Criteria):**
  - Exit code = 0 (Build Success).
  - 0 compilation errors, 0 deprecated API warnings.
  - Tương thích 100% Java 17+ LTS bytecode.

---

### Gate 2: Code Coverage Gate (C0 - Line, C1 - Branch, C2 - Path/Condition)
* **Mục tiêu:** Đảm bảo toàn bộ logic tài chính, parser, security sanitizer đều được kiểm thử tự động.
* **Lệnh thực thi:** `mvn clean test jacoco:report`
* **Tiêu chí Đạt (Pass Criteria):**
  - **C0 (Line Coverage):** $\ge 95\%$ trên toàn bộ package `com.company.taxlibrary.*`.
  - **C1 (Branch Coverage):** $\ge 90\%$ (đặc biệt các nhánh `if/else` xử lý định dạng VAT và lỗi CSV).
  - **C2 (Decision/Path Coverage):** $\ge 90\%$ (kiểm thử đường đi trong `SecuritySanitizer` và `HeaderNormalizer`).
  - 100% Unit Tests & Integration Tests PASS (0 failures, 0 errors, 0 skipped).

---

### Gate 3: Static Code Analysis & SpotBugs Gate
* **Mục tiêu:** Phát hiện sớm các lỗi Clean Code, NullPointer, và code smells.
* **Lệnh thực thi:** `mvn spotbugs:check pmd:check`
* **Tiêu chí Đạt (Pass Criteria):**
  - **SpotBugs:** 0 High/Medium Priority Bugs.
  - **PMD:** Cognitive Complexity $\le 8$ cho mỗi method.
  - 100% `try-with-resources` cho các IO Stream/Reader.
  - Zero raw primitive floating-point (`double`/`float`) cho tính toán tiền tệ.

---

### Gate 4: Security Vulnerability & Anti-Injection Gate
* **Mục tiêu:** Đảm bảo không dính lỗ hổng bảo mật thư viện phụ thuộc và chống CSV Injection / Path Traversal.
* **Lệnh thực thi:** `mvn org.owasp:dependency-check-maven:check`
* **Tiêu chí Đạt (Pass Criteria):**
  - **OWASP Scan:** 0 lỗ hổng có điểm CVSS $\ge 7.0$ (Critical/High).
  - **CSV Formula Injection:** 100% các ký tự `=`, `+`, `-`, `@`, `\t`, `\r` được neutralize bằng tiền tố `'`.
  - **Path Traversal:** Ném `SecurityException` khi input path vượt ra ngoài base directory.

---

### Gate 5: Performance & Memory SLA Gate
* **Mục tiêu:** Đảm bảo thư viện chạy siêu tốc và không gây đè RAM ứng dụng tích hợp.
* **Lệnh thực thi:** `mvn test -Dtest=PerformanceBenchmarkTest`
* **Tiêu chí Đạt (Pass Criteria):**
  - **Throughput:** Xử lý 50,000 dòng CSV trong $\le 200\text{ms}$.
  - **Memory Footprint:** Peak RAM $\le 50\text{MB}$ (nhờ cơ chế Streaming Iteration).
  - **Anti-DoS:** Tự động chặn file $> 50\text{MB}$ hoặc $> 500,000$ dòng.

---

### Gate 6: Public API & Client Backward Compatibility Gate
* **Mục tiêu:** Đảm bảo giao diện gọi hàm tuân thủ hợp đồng Fluent API đã đàm phán với khách hàng.
* **Lệnh thực thi:** `mvn verify -Dtest=ClientApiCompatibilityTest`
* **Tiêu chí Đạt (Pass Criteria):**
  - Lớp `TaxProcessor` cung cấp đầy đủ `.builder().withMetadata(...).process(...)`.
  - Tất cả DTOs (`TaxItemInput`, `TaxItemOutput`, `TaxSummaryReport`) là Java `record` Immutable.
  - File JAR tạo ra tại `target/java-csv-tax-library-1.0.0.jar` kèm file Javadoc và README.

---

## 🛠️ GitHub Actions Workflow Schema (`.github/workflows/ci.yml`)

File cấu hình tự động hóa toàn bộ 6 gates trên GitHub repository của bạn:

```yaml
name: Shared Tax Library CI Quality Gates

on:
  push:
    branches: [ "main", "master", "develop" ]
  pull_request:
    branches: [ "main", "master" ]

jobs:
  build-and-verify:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout Code
      uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Gate 1 - Build & Strict Compilation
      run: mvn clean compile -DcompilerArgument="-Xlint:all"

    - name: Gate 2 - Automated Tests & Coverage (C0/C1/C2)
      run: mvn test jacoco:report

    - name: Gate 3 - Static Code Analysis (SpotBugs & PMD)
      run: mvn spotbugs:check pmd:check

    - name: Gate 4 - OWASP Dependency Security Check
      run: mvn org.owasp:dependency-check-maven:check

    - name: Gate 5 - Performance SLA Benchmark
      run: mvn test -Dtest=PerformanceBenchmarkTest

    - name: Gate 6 - Package & Generate Client Artifacts
      run: mvn package

    - name: Upload JaCoCo Coverage Report
      uses: actions/upload-artifact@v3
      with:
        name: jacoco-coverage-report
        path: target/site/jacoco/

    - name: Upload Built JAR Library
      uses: actions/upload-artifact@v3
      with:
        name: java-tax-library-jar
        path: target/*.jar
```
