import os
import re

base_dir = r"c:\Users\SKYLAP.VN\Documents\dev\task-flow-mini\backend"

# 1. Update POM
pom_path = os.path.join(base_dir, "pom.xml")
with open(pom_path, "r", encoding="utf-8") as f:
    pom_content = f.read()

# Remove commons-csv
pom_content = re.sub(
    r"<dependency>\s*<groupId>org\.apache\.commons</groupId>\s*<artifactId>commons-csv</artifactId>\s*<version>.*?</version>\s*</dependency>",
    "", pom_content, flags=re.DOTALL
)

# Add csv-processing-core if not exists
if "csv-processing-core" not in pom_content:
    dep = """
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>csv-processing-core</artifactId>
            <version>1.0.0</version>
        </dependency>
"""
    pom_content = re.sub(r'(</dependencies>)', dep + r'\1', pom_content)

with open(pom_path, "w", encoding="utf-8") as f:
    f.write(pom_content)

# 2. Delete old CSV files
files_to_delete = [
    r"src\main\java\com\taskflow\infrastructure\csv\TaskCsvReader.java",
    r"src\main\java\com\taskflow\infrastructure\csv\TaskCsvWriter.java",
    r"src\main\java\com\taskflow\infrastructure\csv\PerformanceCsvWriter.java",
    r"src\main\java\com\taskflow\infrastructure\csv\CsvColumnMappingConfig.java"
]
for p in files_to_delete:
    full = os.path.join(base_dir, p)
    if os.path.exists(full):
        os.remove(full)

# 3. Create TaskRecordCalculator and TaskPerformanceCalculator, and update Services
new_files = {
    r"src\main\java\com\taskflow\infrastructure\csv\TaskRecordCalculator.java": """package com.taskflow.infrastructure.csv;
import com.company.csvengine.calc.RecordCalculator;
import com.taskflow.domain.model.Priority;
import com.taskflow.domain.model.Task;
import com.taskflow.domain.model.BoardColumn;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskRecordCalculator implements RecordCalculator<Task> {
    private final UUID boardId;
    private final List<BoardColumn> columns;
    private final Long defaultColumnId;

    public TaskRecordCalculator(UUID boardId, List<BoardColumn> columns, Long defaultColumnId) {
        this.boardId = boardId;
        this.columns = columns;
        this.defaultColumnId = defaultColumnId;
    }

    @Override
    public Task calculate(Map<String, String> rawRow) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setBoardId(boardId);
        task.setCreatedAt(Instant.now());

        String title = rawRow.get("title");
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        task.setTitle(title);

        String assignee = rawRow.get("assignee");
        if (assignee != null && !assignee.trim().isEmpty()) {
            try {
                task.setAssigneeId(Long.valueOf(assignee.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid assignee ID format");
            }
        }

        String dueDate = rawRow.get("dueDate");
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            task.setDueDate(Instant.parse(dueDate.trim()));
        }

        String statusName = rawRow.get("status");
        if (statusName != null && !statusName.trim().isEmpty()) {
            Long matchedColumnId = null;
            for (BoardColumn col : columns) {
                if (col.getName().trim().equalsIgnoreCase(statusName.trim())) {
                    matchedColumnId = col.getId();
                    break;
                }
            }
            if (matchedColumnId == null) {
                throw new IllegalArgumentException("Status column not found: " + statusName);
            }
            task.setStatusColumnId(matchedColumnId);
        } else {
            task.setStatusColumnId(defaultColumnId);
        }

        String priority = rawRow.get("priority");
        if (priority != null && !priority.trim().isEmpty()) {
            try {
                task.setPriority(Priority.valueOf(priority.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid priority: " + priority);
            }
        }
        return task;
    }

    public Map<String, String> toRow(Task task, Map<Long, String> colMap) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("ID", task.getId() != null ? String.valueOf(task.getId()) : "");
        map.put("title", task.getTitle());
        map.put("assignee", task.getAssigneeId() != null ? String.valueOf(task.getAssigneeId()) : "");
        String statusName = task.getStatusColumnId() != null ? colMap.getOrDefault(task.getStatusColumnId(), String.valueOf(task.getStatusColumnId())) : "";
        map.put("status", statusName);
        map.put("priority", task.getPriority() != null ? task.getPriority().name() : "");
        map.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : "");
        return map;
    }
}
""",
    r"src\main\java\com\taskflow\infrastructure\csv\TaskPerformanceCalculator.java": """package com.taskflow.infrastructure.csv;
import com.company.csvengine.calc.RecordCalculator;
import com.taskflow.domain.model.AssigneePerformance;
import com.taskflow.domain.model.Task;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskPerformanceCalculator implements RecordCalculator<AssigneePerformance> {
    @Override
    public AssigneePerformance calculate(Map<String, String> rawRow) {
        throw new UnsupportedOperationException("Performance calculation requires grouped tasks from DB, not CSV rows.");
    }

    public List<AssigneePerformance> computePerformance(List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId))
                .entrySet().stream()
                .map(entry -> {
                    Long assigneeId = entry.getKey();
                    List<Task> userTasks = entry.getValue();
                    int total = userTasks.size();
                    int onTime = 0, late = 0, overdue = 0, completedCount = 0;
                    double totalCompletionHours = 0.0;
                    
                    for (Task t : userTasks) {
                        if (t.getCompletedAt() != null) {
                            if (t.getDueDate() != null && !t.getCompletedAt().isAfter(t.getDueDate())) onTime++;
                            else if (t.getDueDate() != null && t.getCompletedAt().isAfter(t.getDueDate())) late++;
                            else if (t.getDueDate() == null) onTime++;
                            
                            if (t.getCreatedAt() != null) {
                                totalCompletionHours += Duration.between(t.getCreatedAt(), t.getCompletedAt()).toMinutes() / 60.0;
                                completedCount++;
                            }
                        } else {
                            if (t.getDueDate() != null && t.getDueDate().isBefore(Instant.now())) overdue++;
                        }
                    }
                    
                    double rate = total > 0 ? (double) onTime / total : 0.0;
                    double avgHours = completedCount > 0 ? totalCompletionHours / completedCount : 0.0;
                    String fullName = "User " + assigneeId; 
                    return new AssigneePerformance(assigneeId, fullName, total, onTime, late, overdue, rate, avgHours);
                })
                .collect(Collectors.toList());
    }

    public Map<String, String> toRow(AssigneePerformance perf) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("Assignee ID", perf.getAssigneeId() != null ? String.valueOf(perf.getAssigneeId()) : "");
        map.put("Full Name", perf.getFullName());
        map.put("Total Assigned", String.valueOf(perf.getTotalAssigned()));
        map.put("Completed On Time", String.valueOf(perf.getCompletedOnTime()));
        map.put("Completed Late", String.valueOf(perf.getCompletedLate()));
        map.put("Overdue Open", String.valueOf(perf.getOverdueOpen()));
        map.put("On Time Rate", String.valueOf(perf.getOnTimeRate()));
        map.put("Avg Completion Hours", String.valueOf(perf.getAvgCompletionHours()));
        return map;
    }
}
""",
    r"src\main\java\com\taskflow\application\service\TaskCsvImportExportApplicationService.java": """package com.taskflow.application.service;
import com.taskflow.application.port.in.ExportTasksUseCase;
import com.taskflow.application.port.in.ImportTasksUseCase;
import com.taskflow.application.port.out.TaskRepositoryPort;
import com.taskflow.domain.repository.BoardColumnRepositoryPort;
import com.taskflow.domain.model.Task;
import com.taskflow.domain.model.BoardColumn;
import com.taskflow.domain.model.TaskImportError;
import com.taskflow.domain.model.TaskImportResult;
import com.taskflow.infrastructure.csv.TaskRecordCalculator;
import com.company.csvengine.config.CsvColumnMappingConfig;
import com.company.csvengine.parser.CsvReaderEngine;
import com.company.csvengine.parser.CsvWriterEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskCsvImportExportApplicationService implements ImportTasksUseCase, ExportTasksUseCase {
    private static final Logger log = LoggerFactory.getLogger(TaskCsvImportExportApplicationService.class);
    private final TaskRepositoryPort taskRepositoryPort;
    private final BoardColumnRepositoryPort boardColumnRepositoryPort;

    public TaskCsvImportExportApplicationService(TaskRepositoryPort taskRepositoryPort, BoardColumnRepositoryPort boardColumnRepositoryPort) {
        this.taskRepositoryPort = taskRepositoryPort;
        this.boardColumnRepositoryPort = boardColumnRepositoryPort;
    }

    @Override
    public TaskImportResult importFromCsv(UUID boardId, InputStream csv, InputStream metadataJsonOrNull) {
        TaskImportResult result = new TaskImportResult();
        try {
            CsvColumnMappingConfig config;
            if (metadataJsonOrNull != null) {
                config = CsvColumnMappingConfig.fromJson(new InputStreamReader(metadataJsonOrNull, StandardCharsets.UTF_8));
            } else {
                try (InputStream defaultStream = getClass().getResourceAsStream("/csv-mapping/task-mapping.default.json")) {
                    config = CsvColumnMappingConfig.fromJson(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                }
            }

            List<BoardColumn> columns = boardColumnRepositoryPort.findByBoardIdOrderByPositionAsc(boardId);
            Long defaultColumnId = columns.isEmpty() ? null : columns.get(0).getId();
            TaskRecordCalculator calculator = new TaskRecordCalculator(boardId, columns, defaultColumnId);
            
            List<Task> toSave = new ArrayList<>();
            final int[] totalRows = {0};
            final int[] successCount = {0};

            csv.mark(3);
            int ch = csv.read();
            if (ch != 0xEF) csv.reset(); 
            else { csv.read(); csv.read(); }

            new CsvReaderEngine(config).read(
                new InputStreamReader(csv, StandardCharsets.UTF_8),
                rawRow -> {
                    totalRows[0]++;
                    try {
                        Task task = calculator.calculate(rawRow);
                        toSave.add(task);
                        successCount[0]++;
                    } catch (Exception ex) {
                        log.warn("Failed to parse row {}: {}", totalRows[0], ex.getMessage());
                        result.addError(new TaskImportError(totalRows[0], rawRow.toString(), ex.getMessage()));
                    }
                },
                warning -> {}
            );
            
            if (!toSave.isEmpty()) taskRepositoryPort.saveAll(toSave);
            result.setTotalRows(totalRows[0]);
            result.setSuccessCount(successCount[0]);
        } catch (Exception e) {
            log.error("Error during CSV import", e);
            result.addError(new TaskImportError(-1, "", "Failed to process CSV: " + e.getMessage()));
        }
        return result;
    }

    @Override
    public byte[] exportToCsv(UUID boardId, InputStream metadataJsonOrNull) {
        List<Task> tasks = taskRepositoryPort.searchTasksByBoardIds(List.of(boardId), null, null, null);
        CsvColumnMappingConfig config;
        try {
            if (metadataJsonOrNull != null) config = CsvColumnMappingConfig.fromJson(new InputStreamReader(metadataJsonOrNull, StandardCharsets.UTF_8));
            else {
                try (InputStream defaultStream = getClass().getResourceAsStream("/csv-mapping/task-mapping.default.json")) {
                    config = CsvColumnMappingConfig.fromJson(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) { throw new RuntimeException("Failed to load metadata", e); }
        
        List<BoardColumn> columns = boardColumnRepositoryPort.findByBoardIdOrderByPositionAsc(boardId);
        Map<Long, String> colMap = columns.stream().collect(Collectors.toMap(BoardColumn::getId, BoardColumn::getName));
        TaskRecordCalculator calculator = new TaskRecordCalculator(boardId, columns, null);
        
        List<Map<String, String>> rows = tasks.stream().map(t -> calculator.toRow(t, colMap)).collect(Collectors.toList());
        List<String> headers = List.of("ID", getPrimaryAlias(config, "title"), getPrimaryAlias(config, "assignee"), 
                                       getPrimaryAlias(config, "status"), getPrimaryAlias(config, "priority"), getPrimaryAlias(config, "dueDate"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            new CsvWriterEngine().write(rows, writer, headers, config.getCsvDelimiter().charAt(0));
            writer.flush();
        } catch (Exception e) { throw new RuntimeException("CSV export failed", e); }
        return baos.toByteArray();
    }
    
    private String getPrimaryAlias(CsvColumnMappingConfig config, String logicalField) {
        List<String> aliases = config.getColumnMapping().get(logicalField);
        if (aliases != null && !aliases.isEmpty()) return aliases.get(0);
        return logicalField;
    }
}
""",
    r"src\main\java\com\taskflow\application\service\PerformanceApplicationService.java": """package com.taskflow.application.service;
import com.taskflow.application.port.in.ExportPerformanceReportUseCase;
import com.taskflow.domain.repository.TaskRepositoryPort;
import com.taskflow.domain.model.AssigneePerformance;
import com.taskflow.domain.model.Task;
import com.taskflow.infrastructure.csv.TaskPerformanceCalculator;
import com.company.csvengine.parser.CsvWriterEngine;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PerformanceApplicationService implements ExportPerformanceReportUseCase {
    private final TaskRepositoryPort taskRepositoryPort;
    public PerformanceApplicationService(TaskRepositoryPort taskRepositoryPort) { this.taskRepositoryPort = taskRepositoryPort; }

    @Override
    public byte[] exportPerformanceCsv(UUID boardId, Instant from, Instant to) {
        List<Task> tasks = taskRepositoryPort.searchTasksByBoardIds(List.of(boardId), null, from, to);
        TaskPerformanceCalculator calculator = new TaskPerformanceCalculator();
        List<Map<String, String>> rows = calculator.computePerformance(tasks).stream()
                .map(calculator::toRow).collect(Collectors.toList());
                
        List<String> headers = List.of("Assignee ID", "Full Name", "Total Assigned", "Completed On Time",
                                 "Completed Late", "Overdue Open", "On Time Rate", "Avg Completion Hours");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
            new CsvWriterEngine().write(rows, writer, headers, ',');
            writer.flush();
        } catch (Exception e) { throw new RuntimeException("CSV export failed", e); }
        return baos.toByteArray();
    }
}
"""
}

for rel_path, content in new_files.items():
    full = os.path.join(base_dir, rel_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content)

print("task-flow-backend updated to depend on csv-core!")
