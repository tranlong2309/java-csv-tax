# Java Coding & Logging Rules (WorkOrder Module)

> Context files: docs/coding-rules.md, docs/api-rules.md, docs/security-rules.md
> Applies to: All Java/Spring Boot code in this project.


# Java Coding Convention (Standard)

This document defines the standard Java coding convention to be followed in all Java source files in this project. The goal is to improve readability, consistency, maintainability, and safe software development.

---

## 1. General Principles

1. Write code that is easy to read, easy to review, and easy to maintain.
2. Prefer clarity over cleverness.
3. Keep methods small and focused on a single responsibility.
4. Use consistent naming and formatting across the entire project.
5. Avoid duplicated logic and unnecessary complexity.

---

## 2. Naming Conventions

### 2.1 Packages

- Use lowercase only.
- Use reverse domain naming style.
- Keep package names short and meaningful.

Example:

```java
package com.taskflow.domain.model;
package com.taskflow.infrastructure.persistence;
```

### 2.2 Classes, Interfaces, and Enums

- Use UpperCamelCase.
- Class names should be nouns or noun phrases.
- Interface names should describe a capability or contract.
- Enum names should use UpperCamelCase.

Example:

```java
public class BoardService { }
public interface UserRepository { }
public enum TaskStatus { TODO, IN_PROGRESS, DONE }
```

### 2.3 Methods

- Use lowerCamelCase.
- Use verb-based names that clearly describe the action.
- Prefer descriptive names over short abbreviations.

Example:

```java
public Board createBoard(BoardRequest request) {
    return boardRepository.save(new Board());
}

public void updateTaskStatus(Long taskId, TaskStatus status) {
    // method logic
}
```

### 2.4 Variables and Parameters

- Use lowerCamelCase.
- Use meaningful names that reflect business or technical intent.
- Avoid single-letter names except for common loop indexes such as i, j, k.

Example:

```java
String taskTitle;
int retryCount;
for (int i = 0; i < tasks.size(); i++) {
    // valid short index usage
}
```

### 2.5 Constants

- Use UPPER_SNAKE_CASE.
- Constants must be declared as `static final`.

Example:

```java
public static final int MAX_RETRY_COUNT = 3;
public static final String DEFAULT_STATUS = "TODO";
```

### 2.6 Boolean Variables

- Boolean names should read like a question or condition.

Example:

```java
boolean isActive;
boolean hasPermission;
boolean shouldRetry;
```

---

## 3. Formatting Rules

### 3.1 Indentation

- Use 4 spaces for indentation.
- Do not use tabs.

### 3.2 Braces

- Opening brace must be placed on the same line as the declaration or conditional.
- Closing brace must align with the opening declaration.

Example:

```java
if (isValid) {
    doSomething();
} else {
    doFallback();
}
```

### 3.3 Blank Lines

- Use blank lines to separate logical sections of code.
- Keep one blank line between methods.
- Keep one blank line between field declarations and method implementations.

### 3.4 Line Length

- Prefer lines not exceeding 120 characters.
- Break long statements into multiple lines when needed.

### 3.5 One Statement Per Line

- Do not combine multiple statements on one line.
- Keep the code readable and easy to follow.

### 3.6 File Layout

- One public class per file.
- Keep file names matching the public class name.
- Organize imports in a clean and alphabetical order.

---

## 4. Comments and Documentation

- Write comments only when they add value.
- Do not add obvious comments that restate the code.
- Prefer self-explanatory code over excessive documentation.
- Use Javadoc for public classes, public methods, and public constants when needed.

Example:

```java
/**
 * Creates a new board with the given request payload.
 *
 * @param request the board creation request
 * @return the created board
 */
public Board createBoard(BoardRequest request) {
    // implementation
    return board;
}
```

---

## 5. Control Flow and Logic

### 5.1 Avoid Deep Nesting

- Do not write deeply nested conditions.
- Extract logic into helper methods when needed.

### 5.2 Use Clear Conditions

- Keep logical expressions understandable.
- Avoid confusing negations or complex nested ternary expressions.

Example:

```java
if (isActive && hasPermission) {
    grantAccess();
}
```

### 5.3 No Magic Numbers

- Replace numeric literals with named constants when they carry business meaning.

Example:

```java
private static final int MAX_TASKS_PER_BOARD = 50;

if (taskCount > MAX_TASKS_PER_BOARD) {
    throw new IllegalArgumentException("Board is full");
}
```

---

## 6. Exceptions and Error Handling

- Throw specific exceptions, not generic ones.
- Catch only the exceptions you can handle meaningfully.
- Do not suppress exceptions silently.
- Log meaningful error messages when failures occur.

Example:

```java
if (board == null) {
    throw new IllegalArgumentException("Board must not be null");
}

try {
    repository.save(board);
} catch (DataAccessException ex) {
    log.error("Failed to save board: {}", board.getId(), ex);
    throw ex;
}
```

Bad example:

```java
catch (Exception e) {
    return null;
}
```

---

## 7. Logging Standard

- Use SLF4J for logging.
- Do not use `System.out.println` in application code.
- Do not log sensitive or personal information.
- Use parameterized logging rather than string concatenation.

Example:

```java
private static final Logger log = LoggerFactory.getLogger(BoardService.class);

log.info("Board created successfully: id={}", board.getId());
log.warn("Task status update failed for taskId={}", taskId);
```

Bad example:

```java
System.out.println("Created board: " + board);
log.info("User password: " + password);
```

---

## 8. Null Handling

- Avoid returning null from public methods when a better alternative exists.
- Prefer `Optional` for nullable results.
- Validate inputs early and fail fast.

Example:

```java
public Optional<Board> findById(Long id) {
    return boardRepository.findById(id);
}
```

---

## 9. Object Design and Encapsulation

- Keep fields private unless there is a strong reason otherwise.
- Expose behavior through methods, not by leaking internal state.
- Prefer final fields where appropriate.
- Minimize mutability unless the design requires it.

Example:

```java
public class Task {
    private final Long id;
    private String title;

    public Task(Long id, String title) {
        this.id = id;
        this.title = title;
    }
}
```

---

## 10. Method Design

- Methods should have one responsibility.
- Keep methods concise and focused.
- Prefer small helper methods over large monolithic methods.

Good example:

```java
public Task createTask(TaskRequest request) {
    validateTask(request);
    Task task = buildTask(request);
    return taskRepository.save(task);
}
```

---

## 11. Dependency Injection and Construction

- Prefer constructor injection for required dependencies.
- Keep classes easy to test.
- Avoid using field injection in production code.

Example:

```java
public class BoardService {
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }
}
```

---

## 12. Collections and Loops

- Use enhanced `for` loops when possible.
- Do not declare variables inside loops if they are reused outside the loop.
- Avoid unnecessary object creation inside loops.

Example:

```java
String taskTitle;
for (Task task : tasks) {
    taskTitle = task.getTitle();
    System.out.println(taskTitle);
}
```

---

## 13. Code Simplicity

- Prefer simple solutions over abstract or over-engineered designs.
- Avoid unnecessary design patterns unless they clearly solve a real problem.
- Keep the code aligned with the actual project requirements.

---

## 14. Testing Conventions

- Write unit tests for public behavior.
- Cover both normal and edge cases.
- Use clear test names that describe the scenario.

Example:

```java
@Test
void createBoard_shouldReturnBoard_whenRequestIsValid() {
    // arrange
    // act
    // assert
}
```

---

## 15. Standard Summary

The Java source code in this project must follow these core standards:

- UpperCamelCase for classes
- lowerCamelCase for methods and variables
- UPPER_SNAKE_CASE for constants
- lowercase package names
- 4-space indentation
- braces on the same line
- no magic numbers
- no broad exception handling
- no `System.out.println` for application logging
- clear, small, readable methods

Following these rules keeps the code consistent with the accepted Java standard convention and makes the project easier to review, maintain, and extend.
## Rule 1: Language & Framework

Use **Java 17+** and **Spring Boot 3.3+** for all new code.

- **[GOOD]:** `record WorkOrderRequest(String equipmentId, Priority priority) {}`
- **[BAD]:** Using Java 8 `Date` class instead of `java.time.LocalDateTime`.

---

## Rule 2: Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Class / Interface / Enum | `PascalCase` | `WorkOrderService`, `Priority` |
| Method / Variable | `camelCase` | `createWorkOrder()`, `equipmentId` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Package | `lowercase` | `com.posco.mci.controller` |

- **[GOOD]:** `private static final int MAX_RETRY_COUNT = 3;`
- **[BAD]:** `private static final int maxRetryCount = 3;` or `private static final int MaxRetryCount = 3;`

---

## Rule 3: Exception Handling

Never throw or catch generic `RuntimeException` or `Exception`. Use specific custom exceptions or standard ones.

- **[GOOD]:**
```java
if (equipmentId == null || equipmentId.isBlank()) {
    throw new IllegalArgumentException("equipmentId must not be blank");
}
throw new EntityNotFoundException("Equipment not found: " + equipmentId);
```

- **[BAD]:**
```java
throw new RuntimeException("something went wrong");
catch (Exception e) { /* silently swallow */ }
```

---

## Rule 4: Logging Standards — No PII

- Use **SLF4J** logger instantiated as: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- **DO NOT** log sensitive data or PII: raw passwords, emails, phone numbers, national IDs, full equipment owner info.
- Use structured placeholders `{}` instead of string concatenation in log statements.

- **[GOOD]:**
```java
private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
log.info("Work order created: id={}, equipmentId={}", order.getId(), order.getEquipmentId());
```

- **[BAD]:**
```java
System.out.println("Created for: " + request.getOwnerEmail());
log.info("Password used: " + user.getPassword());
Logger logger = Logger.getLogger("MyLogger"); // java.util.logging - not SLF4J
```

---

## Rule 5: Dependency Injection — Constructor Injection Only

Always use **constructor injection**. Never use field injection (`@Autowired` on fields).

- **[GOOD]:**
```java
public class WorkOrderService {
    private final WorkOrderRepository repository;
    private final EquipmentService equipmentService;

    public WorkOrderService(WorkOrderRepository repository, EquipmentService equipmentService) {
        this.repository = repository;
        this.equipmentService = equipmentService;
    }
}
```

- **[BAD]:**
```java
@Autowired
private WorkOrderRepository repository; // Field injection - avoid

@Autowired
private EquipmentService equipmentService;
```

---

## Rule 6: Code Simplicity — No Over-Engineering

Avoid adding factories, abstract layers, or design patterns unless explicitly requested. Write clean, flat vertical slices.

- **[GOOD]:** One service class per feature. Direct call from controller to service to repository.
- **[BAD]:** Creating `WorkOrderFactory`, `AbstractWorkOrderHandler`, `WorkOrderHandlerRegistry` for a simple CRUD.

---

## Rule 7: Variable Declaration Scope — Declare Outside Loops

**DO NOT** declare variables inside `for`, `while`, or `do-while` loops. Declare all loop-related variables before the loop block.

- **[GOOD]:**
```java
String item = "";
int digitSum = 0;
for (int i = 0; i < list.size(); i++) {
    item = list.get(i);
    digitSum = process(item);
}
```

- **[BAD]:**
```java
for (int i = 0; i < list.size(); i++) {
    String item = list.get(i);   // declared inside - BAD
    int digitSum = process(item); // declared inside - BAD
}
```

---

## Rule 8: Method Length & Single Responsibility

Each method should do **one thing** and be at most **30-40 lines**. Extract sub-logic into private helper methods with descriptive names.

- **[GOOD]:**
```java
public WorkOrderResponse createWorkOrder(WorkOrderRequest req, String createdBy) {
    validateEquipmentActive(req.getEquipmentId());
    WorkOrder order = buildWorkOrder(req, createdBy);
    WorkOrder saved = repository.save(order);
    return toResponse(saved);
}
```

- **[BAD]:** One giant 150-line `createWorkOrder` method doing validation + mapping + persistence + notification.

---

## Rule 9: Immutability — Prefer Final Fields

Prefer `final` for fields that are not reassigned. Use immutable data structures where possible.

- **[GOOD]:**
```java
public final class MyBigNumber {
    private static final Logger log = LoggerFactory.getLogger(MyBigNumber.class);
    private MyBigNumber() {}
}
```

- **[BAD]:**
```java
public class MyBigNumber {
    private Logger log; // non-final, can be reassigned
}
```

---

## Rule 10: Use Enums for Fixed Value Sets

Never use raw String literals for domain values with a fixed set (status, priority, role). Define as `enum`.

- **[GOOD]:**
```java
public enum Priority { LOW, MEDIUM, HIGH, URGENT }
public enum WorkOrderStatus { NEW, IN_PROGRESS, DONE, CANCELLED }
```

- **[BAD]:**
```java
String priority = "urgent"; // typo-prone, no type safety
if (priority.equals("high")) { ... }
```

---

## Rule 11: Null Safety — No Returning Null from Public Methods

Public methods must never return `null`. Use `Optional<T>` for nullable lookups, or throw a specific exception.

- **[GOOD]:**
```java
public Optional<WorkOrder> findById(Long id) {
    return repository.findById(id);
}
// Caller:
workOrderService.findById(id)
    .orElseThrow(() -> new EntityNotFoundException("WorkOrder not found: " + id));
```

- **[BAD]:**
```java
public WorkOrder findById(Long id) {
    return repository.findById(id).orElse(null); // null returned to caller
}
```

---

## Rule 12: Test Coverage — Every Public Method Must Have Unit Tests

Every public method must have at least one positive test and one negative/edge test.

- **[GOOD]:** `createWorkOrder_shouldReturn201_whenValidRequest()` and `createWorkOrder_shouldReturn400_whenEquipmentIdBlank()`
- **[BAD]:** No test class exists for `WorkOrderService`.
# Java Code Formatting Rules (Line Breaks / Blank Lines)

Applies to all Java code written or modified in `xlsx-invoice-lib`.

## General Principles
- Group statements by meaning and separate groups with exactly one blank line.
- Keep the project's existing style (4-space indent, brace placement, single-line `if` without braces where already used). Do not switch styles on your own.
- If the project has a formatter (Spotless, Checkstyle, `.editorconfig`), follow its configuration.
- When doing a formatting-only refactor: do not change logic, names, statement order, imports, comments, annotations, or Javadoc.

## When to Insert a Blank Line
1. **Variable declarations → main processing step**: after the block of variable declarations, add one blank line before the first `for`/`while`/`if`/`try`.
2. **Within a declaration block**: keep variables with the same purpose together and separate groups with one blank line (e.g. the collections `lines` and `errors` form one group; the totals `beforeTotal`, `vatTotal`, `afterTotal` and the counter form another).
3. **Inside a loop body**, separate by phase:
   - reading the input data
   - computing and skip checks (`if (...) continue;`)
   - building the result (creating objects, incrementing counters, adding to lists)
   - accumulating (the `xxxTotal = xxxTotal.add(...)` lines)
4. **After a large `for`/`while`/`try`/`if` block ends**: one blank line before the next statement.
5. **Before a standalone `return` or `throw`**: one blank line, unless it is the only statement in the block or directly follows a short guard clause.
6. **At the start of a method**: guard clauses / validation (null checks, early `throw`) form one group, followed by one blank line before the main logic.
7. **Between methods, classes, and inner classes**: exactly one blank line.

## Do Not
- Do not add a blank line at the start or end of a `{ }` block.
- Do not use two consecutive blank lines.
- Do not separate tightly related statements (e.g. the three total-accumulation lines must stay together; a variable declaration and the line that immediately uses it must not be split).
- Do not over-split: each group should have 2 or more lines where possible, and a method shorter than 6 lines should be left as is.

## Wrapping Long Lines
- For expressions longer than 120 characters, break at the `.` of a method chain or after the `,` between arguments, indented 8 spaces from the first line. Do not change the content of the expression.

## Reference Example
```java
List<InvoiceLine> lines = new ArrayList<>();
List<RowError> errors = new ArrayList<>();

BigDecimal beforeTotal = BigDecimal.ZERO.setScale(config.scale(), ...);
BigDecimal vatTotal = beforeTotal;
BigDecimal afterTotal = beforeTotal;
int lineNumber = 0;

for (int index = 0; index < items.size(); index++) {
    InvoiceItem item = items.get(index);
    int rowNumber = index + 1;

    Optional<InvoiceLine> calculation = calculateSingleLine(...);
    if (calculation.isEmpty()) continue;

    InvoiceLine line = calculation.get();
    lineNumber++;
    lines.add(new InvoiceLine(lineNumber, ...));

    beforeTotal = beforeTotal.add(line.amountBeforeVat());
    vatTotal = vatTotal.add(line.vatAmount());
    afterTotal = afterTotal.add(line.amountAfterVat());
}

if (exceedsPrecision(beforeTotal) || exceedsPrecision(vatTotal)) {
    throw new InvoiceException(...);
}
```

## When Refactoring Formatting Only
- Process one file at a time, starting with the longest methods.
- Verify when done:
  - `git diff -w --stat` must be empty (no changes other than whitespace).
  - `mvn -B -ntp clean verify` in `xlsx-invoice-lib` must pass.
- Use a separate commit for the formatting pass (e.g. `style: format blank lines`) and never mix it with logic changes.
- If unsure whether to split somewhere, leave it unchanged and list it in the report instead of guessing.