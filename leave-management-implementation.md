# 7. Implementation — Leave Management Module

This section documents the Leave Management Module: employee leave balance, application, and history (FR11–FR13); HR approve/reject (FR7); and concurrency controls for leave balances (NFR7). Of NFR6/NFR8, **NFR8 (scalability)** is the stronger fit for this module; NFR6 is noted as an inherited system property.

---

## Components Implemented

Leave management spans `common` (remote API), `server` (rules + persistence), and `client` (employee/HR Swing UIs calling RMI asynchronously).

### Remote interface (`common.HRMService`)

Leave operations on `HRMService` each declare `throws RemoteException`:

```java
int getLeaveBalance(int empId, String leaveType) throws RemoteException;
String applyLeave(int empId, LeaveApplication application) throws RemoteException;
List<LeaveApplication> getLeaveHistory(int empId) throws RemoteException;
List<LeaveApplication> getPendingLeaves() throws RemoteException;
boolean updateLeaveStatus(int leaveId, String status) throws RemoteException;
```

### Server business logic (`HRMServiceImpl`)

**FR12 — Apply leave.** `applyLeave` is `synchronized`. It sets employee ID and `PENDING` status, validates dates, calculates inclusive days, and for `ANNUAL`/`SICK` rejects insufficient balance before insert:

```java
@Override
public synchronized String applyLeave(int empId, LeaveApplication application)
        throws RemoteException {
    application.setEmpId(empId);
    application.setStatus("PENDING");
    validateLeaveDates(application);
    int requestedDays = calculateLeaveDays(application);
    String leaveType = application.getLeaveType();

    if ("ANNUAL".equalsIgnoreCase(leaveType) || "SICK".equalsIgnoreCase(leaveType)) {
        int balance = db.getLeaveBalance(empId, leaveType);
        if (requestedDays > balance) {
            throw new RemoteException("Insufficient " + leaveType.toLowerCase()
                    + " leave balance. Requested: " + requestedDays
                    + ", Available: " + balance);
        }
    }
    int leaveId = db.insertLeaveApplication(application);
    return "Leave application submitted successfully. Reference ID: " + leaveId;
}
```

Date validation and day calculation run on the server so UI-only clients cannot bypass the rules:

```java
private void validateLeaveDates(LeaveApplication application) throws RemoteException {
    if (application.getStartDate() == null || application.getEndDate() == null) {
        throw new RemoteException("Start date and end date are required.");
    }
    if (application.getEndDate().before(application.getStartDate())) {
        throw new RemoteException("End date cannot be before start date.");
    }
}

private int calculateLeaveDays(LeaveApplication application) {
    return (int) ChronoUnit.DAYS.between(
            application.getStartDate().toLocalDate(),
            application.getEndDate().toLocalDate()) + 1;
}
```

**FR7 — Approve/reject.** `updateLeaveStatus` is also `synchronized`. On `APPROVED`, the server loads the pending row, computes days, and deducts annual/sick balance before updating status. Rejection skips deduction; emergency leave has no balance column.

```java
@Override
public synchronized boolean updateLeaveStatus(int leaveId, String status)
        throws RemoteException {
    if ("APPROVED".equalsIgnoreCase(status)) {
        LeaveApplication leave = findPendingLeave(leaveId);
        // ...
        if ("ANNUAL".equalsIgnoreCase(leaveType) || "SICK".equalsIgnoreCase(leaveType)) {
            if (!db.deductLeaveBalance(leave.getEmpId(), leaveType, days)) {
                throw new RemoteException(
                        "Failed to deduct leave balance. Insufficient balance...");
            }
        }
    }
    return db.updateLeaveStatus(leaveId, status.toUpperCase());
}
```

**FR11 / FR13.** `getLeaveBalance` and `getLeaveHistory` delegate to `DatabaseManager` and wrap SQL failures as `RemoteException`. HR pending list uses `getPendingLeaves()`.

### Client logic — employee (`EmployeeDashboard`, `LeaveManagementPanel`)

**FR11.** The Leave Balance tab loads annual and sick balances via `SwingWorker` so RMI does not block the EDT, then updates labels in `done()`.

**FR12.** `LeaveManagementPanel` uses LGoodDatePicker with a lower bound of today, validates dates again on submit (past start date, end before start), builds a `LeaveApplication`, and calls `service.applyLeave(...)` in a background worker. Errors from the server (e.g. insufficient balance) surface in a `JOptionPane`.

**FR13.** Leave History fills a non-editable `JTable` from `getLeaveHistory(empId)`, showing type, dates, status, reason, and applied-on timestamp.

### Client logic — HR (`HRDashboard`)

**FR7.** The Approve Leave tab loads `getPendingLeaves()` into a table. Approve/Reject call `updateLeaveStatus(leaveId, "APPROVED"|"REJECTED")` via `SwingWorker`, then refresh the list.

---

## Data Handling & Serialization

Leave data crosses RMI as a serializable DTO. `LeaveApplication` implements `Serializable` with an explicit `serialVersionUID` for client/server compatibility:

```java
public class LeaveApplication implements Serializable {
    private static final long serialVersionUID = 1L;

    private int leaveId;
    private int empId;
    private String leaveType;
    private String status;
    private String reason;
    private Date startDate;
    private Date endDate;
    private Date appliedOn;
    private int approvedBy;
    private Date approvedAt;
    private String hrComment;
    // getters and setters...
}
```

On apply, the client sends type, dates, and reason. The server sets `empId` and `status` before persistence so the client cannot invent another employee’s ID or force `APPROVED`. History and pending results return `List<LeaveApplication>`; RMI serializes the list and each element. Dates use `java.sql.Date` (serializable, maps to MySQL via JDBC). Balance queries return a primitive `int`. One shared `common` class definition is used on every client and server machine.

---

## Security Implementation

Leave-specific security is lighter than authentication (handled elsewhere), but these measures apply:

1. **Transport confidentiality** — Leave payloads use SSL/TLS RMI (`SslRMIClientSocketFactory` / `SslRMIServerSocketFactory`), so details are not sent in plaintext.
2. **SQL injection prevention** — Leave queries use `PreparedStatement` with bound parameters. Balance column names are whitelisted (`ANNUAL` → `annual_leave`, `SICK` → `sick_leave`), never concatenated from raw user input.
3. **Server-side rule enforcement** — Date and balance checks run in `applyLeave` / `updateLeaveStatus`, not only in Swing. HR vs employee screens are separated after login on the client; deeper server-side HR authorization is outside this module’s current scope.

---

## Database/Storage Integration

Persistence is centralized in MySQL via HikariCP. Two tables underpin leave management:

- **`leave_balance`** — one row per employee (`emp_id` PK), with `annual_leave` and `sick_leave` (default 14 each), created when HR registers an employee.
- **`leave_applications`** — stores each request: type (`ANNUAL`/`SICK`/`EMERGENCY`), dates, reason, status (`PENDING`/`APPROVED`/`REJECTED`), `applied_on`, and optional approval metadata (`approved_by`, `approved_at`, `hr_comment`).

Key operations:

| Operation | Persistence behaviour |
|-----------|------------------------|
| View balance | `SELECT annual_leave` / `sick_leave` from `leave_balance` |
| Apply leave | `INSERT` into `leave_applications` with status `PENDING` |
| Leave history | `SELECT` by `emp_id`, ordered by `applied_on DESC` |
| Pending (HR) | `SELECT` where `status = 'PENDING'` |
| Approve | Conditional balance deduct, then `UPDATE` status |

The critical concurrency-safe update is balance deduction:

```java
String sql = """
        UPDATE leave_balance
        SET %s = %s - ?
        WHERE emp_id = ? AND %s >= ?
        """.formatted(column, column, column);
```

If two approvals race, only updates that still have enough remaining days succeed (`executeUpdate() == 1`). Combined with `synchronized` on the remote methods, this prevents over-allocation of leave days.

---

## Non-Functional Requirements Tied to This Module

### NFR7 — Concurrency

Leave approval is the clearest race risk: concurrent HR approvals could otherwise over-deduct the same balance. The module uses two layers:

1. **JVM-level** — `synchronized` on `applyLeave` and `updateLeaveStatus` serializes check-then-act sequences in one server JVM.
2. **Database-level** — conditional `UPDATE ... WHERE balance >= days` makes deduction atomic; failure returns insufficient balance instead of a negative total.

RMI runs each call on its own thread; HikariCP gives each operation its own pooled connection so concurrent balance/history reads stay safe.

### NFR8 — Scalability (primary companion for this module)

**NFR8 is the more relevant of NFR6/NFR8.** Leave load grows with headcount and concurrent clients; the design supports that without redesigning leave logic:

- Balances and applications live in **centralized MySQL** shared by all clients.
- **HikariCP** (pool size 10) handles concurrent leave queries/updates without one connection per thread indefinitely.
- New employees get balance rows at registration; apply/approve need no code changes as headcount grows.
- Host/port and DB URL stay in `config.properties`, so extra client machines point at the same server.

### NFR6 — Heterogeneity (secondary / inherited)

**NFR6 is less leave-specific.** The leave API is pure Java RMI, so clients and servers can run on different OS platforms given a compatible JVM, shared `common` classes, and SSL trust config. Leave Management benefits from that but does not implement platform-specific leave code—use NFR8 as the main NFR narrative with NFR7.

---

## Challenges Faced

1. **Where to enforce rules.** UI date pickers alone were insufficient for a distributed system. Server-side validation ensures balance and date integrity even if another client calls the remote API directly.
2. **Race conditions on approval.** A naïve “read balance, subtract, write” path was unsafe under concurrent HR approvals. `synchronized` remote methods plus conditional SQL closed the gap without a heavier locking framework.
3. **Emergency vs quota leave.** Emergency leave has no balance column; the code skips check/deduct for that type while annual/sick remain quota-controlled.
4. **Inclusive day counting.** Day count (`end − start + 1`) was centralized in `calculateLeaveDays` so UI expectations and server deductions stay aligned.
5. **Responsive Swing UI.** Leave RMI calls run in `SwingWorker` with UI updates only in `done()`, so network/DB latency does not freeze dashboards.

These choices make leave the most stateful multi-step workflow—apply, validate, queue, approve, deduct—while remaining safe under concurrent clients and scalable as headcount grows.
