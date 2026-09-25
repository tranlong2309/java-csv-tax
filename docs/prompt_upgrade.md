BỐI CẢNH — ĐỌC KỸ TRƯỚC KHI SỬA GÌ CẢ

Bản code CSV import/export/performance bạn vừa viết cho project "task-flow-backend" có lỗi nghiêm trọng:
nó được đặt SAI REPO (nằm trong repo thư viện "java-csv-tax", không nằm trong "task-flow-backend"), và
domain model bạn dùng (Task, TaskRepositoryPort, Status enum...) là TỰ BỊA, không khớp với code thật của
project. Vì vậy code hiện tại KHÔNG COMPILE ĐƯỢC. Yêu cầu bắt buộc: TRƯỚC KHI VIẾT BẤT KỲ DÒNG CODE NÀO,
phải tự đọc lại các file sau trong project task-flow-backend thật và dùng ĐÚNG NGUYÊN VĂN field/method có
trong đó — không được đoán, không được giả định, không được tự thêm field mới vào các class đã có sẵn:
- src/main/java/com/taskflow/domain/model/Task.java
- src/main/java/com/taskflow/domain/model/Priority.java
- src/main/java/com/taskflow/domain/model/BoardColumn.java
- src/main/java/com/taskflow/domain/repository/TaskRepositoryPort.java
- src/main/java/com/taskflow/domain/repository/BoardColumnRepositoryPort.java
- src/main/java/com/taskflow/infrastructure/persistence/repository/TaskRepositoryAdapter.java
- src/main/java/com/taskflow/infrastructure/persistence/repository/SpringDataTaskRepository.java
- pom.xml (root của task-flow-backend)

SỰ THẬT VỀ DOMAIN MODEL — bám sát đúng những field/method này, KHÔNG ĐƯỢC SAI:

1. Task.assigneeId là kiểu Long, KHÔNG PHẢI UUID. Toàn bộ code liên quan (mapping CSV, AssigneePerformance,
   group by assignee...) phải dùng Long, không được ép sang UUID.fromString().

2. Task KHÔNG CÓ field "status" hay enum Status nào cả. Trạng thái công việc nằm ở field
   `statusColumnId` (kiểu Long), trỏ tới id của một `BoardColumn` (xem BoardColumn.java: id, boardId,
   name, position). Không có khái niệm "DONE"/"OVERDUE"/"LATE" là chuỗi cố định trong hệ thống này.
   Khi CSV có cột "status" ghi tên cột dạng text (VD "Done", "In Progress"), phải RESOLVE tên đó thành
   statusColumnId thật bằng cách gọi BoardColumnRepositoryPort.findByBoardId(boardId), so khớp
   BoardColumn.getName() (không phân biệt hoa/thường, trim khoảng trắng) để lấy ra getId(). Nếu không
   tìm thấy cột trùng tên, coi là lỗi của dòng đó (thêm vào TaskImportError), KHÔNG được throw làm hỏng
   cả file, KHÔNG được tự bịa ra enum Status.

3. Task.priority là enum Priority có đúng 3 giá trị: LOW, MEDIUM, HIGH (xem Priority.java). Khi parse từ
   CSV, dùng Priority.valueOf(value.trim().toUpperCase()) trong try/catch, bắt lỗi format sai thành
   TaskImportError, không throw thẳng ra ngoài.

4. TaskRepositoryPort nằm ở package com.taskflow.domain.repository (KHÔNG PHẢI application.port.out).
   Các method THẬT SỰ TỒN TẠI trong interface này:
   - Task save(Task task)
   - void saveAll(List<Task> tasks)
   - Optional<Task> findById(UUID id)
   - List<Task> findByStatusColumnIdOrderByPositionAsc(Long statusColumnId)
   - PagedResponse<Task> searchTasks(UUID boardId, Long statusColumnId, Long assigneeId, Priority priority,
     String search, Boolean overdueOnly, int page, int size)
   - List<Task> searchTasksByBoardIds(List<UUID> boardIds, Long assigneeId, Instant from, Instant to)
   - long countByBoardId(UUID boardId)
   - long countByBoardIdAndCompletedAtIsNotNull(UUID boardId)
   - long countByBoardIdAndStatusColumnId(UUID boardId, Long statusColumnId)
   - long countByBoardIdAndIsBlockedTrue(UUID boardId)
   - long countByBoardIdAndDueDateBeforeAndCompletedAtIsNull(UUID boardId, Instant date)

   KHÔNG có method findByBoardId(UUID) hay findByBoardIdAndDateRange(...) — đây là 2 method bạn đã
   BỊA RA ở bản trước, không tồn tại. Để lấy toàn bộ task theo boardId cho export/performance:
   a) Cách ưu tiên: TÁI SỬ DỤNG method có sẵn `searchTasksByBoardIds(List.of(boardId), null, from, to)`
      (truyền assigneeId=null để lấy tất cả, from/to = null hoặc khoảng thời gian rất rộng nếu cần lấy
      hết — kiểm tra implementation thật trong TaskRepositoryAdapter xem null có được xử lý đúng không).
   b) Nếu bước (a) không đáp ứng được nhu cầu export toàn bộ task không giới hạn ngày, ĐƯỢC PHÉP thêm 1
      method mới vào TaskRepositoryPort, nhưng phải implement ĐẦY ĐỦ CẢ 3 TẦNG: thêm khai báo trong
      TaskRepositoryPort.java, thêm implementation trong TaskRepositoryAdapter.java, và thêm query
      method tương ứng trong SpringDataTaskRepository.java. KHÔNG được chỉ thêm vào interface rồi bỏ đó.

5. Task có sẵn 2 field then chốt để tính hiệu suất thật, PHẢI DÙNG thay vì tự nghĩ ra field khác:
   `createdAt` (Instant), `completedAt` (Instant, null nếu chưa xong), `dueDate` (Instant).
   - onTime: completedAt != null && dueDate != null && !completedAt.isAfter(dueDate)
   - late (đã xong nhưng trễ): completedAt != null && dueDate != null && completedAt.isAfter(dueDate)
   - overdueOpen (chưa xong nhưng đã quá hạn): completedAt == null && dueDate != null &&
     dueDate.isBefore(Instant.now())
   - avgCompletionHours: trung bình của Duration.between(createdAt, completedAt).toHours() trên các
     task ĐÃ completedAt != null. BẮT BUỘC PHẢI TÍNH RA GIÁ TRỊ THẬT, không được hard-code 0.0 như bản
     trước — đây là lý do chính của tính năng này.

VỊ TRÍ ĐẶT FILE — BẮT BUỘC, KHÔNG ĐƯỢC LÀM SAI LẦN NỮA

Toàn bộ code phải nằm TRỰC TIẾP TRONG project task-flow-backend thật (thư mục có pom.xml groupId
com.taskflow, artifactId task-flow-backend), theo đúng cấu trúc thư mục đã thống nhất trước đó:

src/main/java/com/taskflow/
├── domain/model/AssigneePerformance.java, TaskCsvRow.java, TaskImportResult.java, TaskImportError.java
├── application/port/in/ImportTasksUseCase.java, ExportTasksUseCase.java, ExportPerformanceReportUseCase.java
├── application/service/TaskCsvImportExportApplicationService.java, PerformanceApplicationService.java
├── infrastructure/csv/CsvColumnMappingConfig.java, TaskCsvReader.java, TaskCsvWriter.java, PerformanceCsvWriter.java
├── infrastructure/web/controller/TaskCsvController.java
├── infrastructure/web/dto/TaskImportResultResponse.java
└── resources/csv-mapping/task-mapping.default.json

TUYỆT ĐỐI KHÔNG được viết bất kỳ file nào vào repo "java-csv-tax" hay bất kỳ project/module riêng nào
khác. Không copy, không fork, không tham chiếu ngược lại package com.company.taxlibrary.* dưới bất kỳ
hình thức nào (không import, không kế thừa, không dùng chung pom.xml).

SỬA CÁC LỖI CỤ THỂ SAU (đã phát hiện ở bản trước, review kỹ để không lặp lại)

1. pom.xml của task-flow-backend: thêm dependency org.apache.commons:commons-csv (bản mới ổn định).
   KHÔNG cần thêm spring-web/slf4j vì project đã có sẵn qua spring-boot-starter-web/logging.

2. CsvColumnMappingConfig: giữ nguyên cách làm tốt của bản trước (Map<String, List<String>> alias,
   so khớp không phân biệt hoa thường, có default JSON) — PHẦN NÀY ĐÚNG HƯỚNG, không cần viết lại từ đầu.

3. TaskCsvReader (import): dùng CsvColumnMappingConfig để lấy đủ 5 cột (title, assignee, status,
   priority, dueDate) như đã làm, giữ nguyên phần xử lý BOM UTF-8 (đã đúng).

4. TaskCsvImportExportApplicationService.mapRowToTask(): PHẢI SET ĐẦY ĐỦ status (resolve qua
   BoardColumnRepositoryPort như mục 2 ở trên) và priority (Priority.valueOf), không được bỏ qua như
   bản trước. Set thêm createdAt = Instant.now() và statusColumnId mặc định (cột đầu tiên theo
   position nếu CSV không có cột status) để Task hợp lệ khi lưu.

5. TaskCsvWriter (export) và ExportTasksUseCase: METADATA PHẢI ÁP DỤNG CHO CẢ EXPORT, không được hard-code
   header cứng như bản trước. Header cột xuất ra phải lấy tên alias ĐẦU TIÊN trong CsvColumnMappingConfig
   cho từng logical field (nếu có metadata truyền vào), fallback về tên mặc định nếu không có metadata.
   Giữ nguyên phần sanitize() chống formula injection (=, +, -, @, tab, \r) — phần này đã đúng.

6. PerformanceApplicationService: tính đúng 4 chỉ số onTime/late/overdueOpen/avgCompletionHours theo
   công thức ở mục "5" phần domain model bên trên, dựa trên completedAt/dueDate/createdAt thật, KHÔNG
   dựa trên field "status" bịa ra. fullName lấy thật từ user service nếu project có sẵn cơ chế tra cứu
   tên nhân viên theo id (kiểm tra xem có UserRepositoryPort hay tương tự không); nếu không có, giữ
   placeholder nhưng phải ghi rõ comment "TODO: cần UserRepositoryPort để lấy tên thật" thay vì im lặng.

7. Viết lại toàn bộ test tương ứng trong src/test/java/com/taskflow/... theo đúng cấu trúc thư mục ở
   trên, dùng dữ liệu Task giả lập đúng kiểu field thật (assigneeId là Long, không phải UUID).

SAU KHI SỬA XONG

Liệt kê rõ: (a) toàn bộ file đã tạo/sửa với đường dẫn đầy đủ, (b) xác nhận danh sách method của
TaskRepositoryPort có bị thêm mới không và đã implement đủ 3 tầng (port/adapter/spring-data) chưa,
(c) xác nhận avgCompletionHours đã được tính bằng dữ liệu thật, không phải hard-code.