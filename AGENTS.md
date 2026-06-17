# agents.md — HRM System (Java RMI + Swing + MySQL)

---

## Project Overview

Build a distributed **Human Resource Management (HRM)** system for BHEL using:
- **Java RMI** for client-server communication
- **Java Swing** for the GUI client
- **MySQL** for the centralized database

---

## Project Structure

```
HRMSystem/
├── src/
│   ├── common/                  # Shared interfaces & models (used by both client & server)
│   │   ├── HRMService.java      # Remote interface
│   │   ├── Employee.java        # Serializable model
│   │   ├── LeaveApplication.java
│   │   └── FamilyDetail.java
│   │
│   ├── server/                  # Server-side code
│   │   ├── HRMServer.java       # Main server entry point (binds RMI registry)
│   │   ├── HRMServiceImpl.java  # Implements HRMService remote interface
│   │   ├── DatabaseManager.java # MySQL JDBC connection & queries
│   │ 
│   │
│   └── client/                  # Client-side GUI code
│       ├── HRMClient.java       # Main client entry point (looks up RMI registry)
│       ├── LoginFrame.java      # Login screen (HR or Employee)
│       ├── HRDashboard.java     # HR staff main panel
│       ├── EmployeeDashboard.java
│       ├── RegisterEmployeePanel.java
│       ├── LeaveManagementPanel.java
│       ├── ProfilePanel.java
│       ├── FamilyDetailsPanel.java
│       └── ReportPanel.java
│
├── db/
│   └── schema.sql               # MySQL schema — run this first
│
│
│
└── README.md
```

---

## Database Schema (MySQL)

```sql
-- db/schema.sql

CREATE DATABASE IF NOT EXISTS hrm_db;
USE hrm_db;

CREATE TABLE employees (
    emp_id        INT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    ic_passport   VARCHAR(50)  NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,    -- store BCrypt hash
    role          ENUM('HR', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE family_details (
    detail_id     INT AUTO_INCREMENT PRIMARY KEY,
    emp_id        INT NOT NULL,
    member_name   VARCHAR(100) NOT NULL,
    relationship  VARCHAR(50)  NOT NULL,
    dob           DATE,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id) ON DELETE CASCADE
);

CREATE TABLE leave_balance (
    emp_id        INT PRIMARY KEY,
    annual_leave  INT DEFAULT 14,
    sick_leave    INT DEFAULT 14,
    FOREIGN KEY (emp_id) REFERENCES employees(emp_id) ON DELETE CASCADE
);

CREATE TABLE leave_applications (
    leave_id       INT AUTO_INCREMENT PRIMARY KEY,
    emp_id         INT NOT NULL,
    leave_type     ENUM('ANNUAL', 'SICK', 'EMERGENCY') NOT NULL,
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    reason         TEXT,

    status         ENUM('PENDING', 'APPROVED', 'REJECTED')
                   DEFAULT 'PENDING',

    applied_on     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- HR approval information
    approved_by    INT NULL,
    approved_at    TIMESTAMP NULL,
    hr_comment     TEXT NULL,

    FOREIGN KEY (emp_id)
        REFERENCES employees(emp_id)
        ON DELETE CASCADE,

    FOREIGN KEY (approved_by)
        REFERENCES employees(emp_id)
        ON DELETE SET NULL
);
```

---

## Remote Interface (Common Layer)

```java
// src/common/HRMService.java
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface HRMService extends Remote {

    // --- HR Functions ---
    String registerEmployee(String firstName, String lastName,
                            String icPassport, String username,
                            String password) throws RemoteException;

    List<Employee> getAllEmployees() throws RemoteException;
    byte[] generateYearlyReport(int empId, int year) throws RemoteException;

    // --- Employee Functions ---
    Employee login(String username, String password) throws RemoteException;
    boolean updateProfile(int empId, Employee updatedData) throws RemoteException;
    boolean addFamilyDetail(int empId, FamilyDetail detail) throws RemoteException;
    List<FamilyDetail> getFamilyDetails(int empId) throws RemoteException;

    // --- Leave Functions ---
    int getLeaveBalance(int empId, String leaveType) throws RemoteException;
    String applyLeave(int empId, LeaveApplication application) throws RemoteException;
    List<LeaveApplication> getLeaveHistory(int empId) throws RemoteException;
    List<LeaveApplication> getPendingLeaves() throws RemoteException;

    // --- HR Leave Approval ---
    boolean updateLeaveStatus(int leaveId, String status) throws RemoteException;

  
}
```

---

## Serializable Models (Common Layer)

All model classes must implement `java.io.Serializable`.

```java
// src/common/Employee.java
public class Employee implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int empId;
    private String firstName, lastName, icPassport, username, role;
    // getters & setters...
}

// src/common/LeaveApplication.java
public class LeaveApplication implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int leaveId, empId;
    private String leaveType, status, reason;
    private java.sql.Date startDate, endDate;
    // getters & setters...
}

// src/common/FamilyDetail.java
public class FamilyDetail implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int detailId, empId;
    private String memberName, relationship;
    private java.sql.Date dob;
    // getters & setters...
}
```

---

## Server Implementation Guidelines

### HRMServer.java
- Start the RMI registry on port **1099** (default)
- Bind the `HRMServiceImpl` instance to the registry name `"HRMService"`
- Load SSL/TLS properties before starting the registry
- Use `LocateRegistry.createRegistry(1099)` or `rmiregistry` process

### HRMServiceImpl.java
- Extends `UnicastRemoteObject`, implements `HRMService`
- Each remote method delegates to `DatabaseManager`
- Use **synchronized** blocks or a connection pool for thread safety (multi-threading)
- Hash passwords using **BCrypt** before storing; verify on login
- Throw meaningful `RemoteException` with descriptive messages on failure

### DatabaseManager.java
- Use `com.mysql.cj.jdbc.Driver` with **JDBC**
- Use a **connection pool** (HikariCP recommended) for thread safety
- Wrap all operations in try-catch; log exceptions server-side
- Provide methods: `insertEmployee()`, `getEmployeeById()`, `insertLeave()`, `updateLeaveStatus()`, etc.

---

## Client Implementation Guidelines

### HRMClient.java
- Look up the remote object: `Naming.lookup("rmi://localhost/HRMService")`
- Set `javax.net.ssl` trust store properties before lookup
- Pass the `HRMService` stub to GUI frames as a constructor argument
- Catch `RemoteException` and show user-friendly `JOptionPane` error dialogs

### GUI Frames & Panels (Java Swing)

| Class | Purpose |
|---|---|
| `LoginFrame` | Username + password fields, role selector (HR / Employee), login button |
| `HRDashboard` | Tabbed pane: Register Employee, View All Employees, Approve Leave, Generate Report |
| `EmployeeDashboard` | Tabbed pane: My Profile, Family Details, Leave Balance, Apply Leave, Leave History |
| `RegisterEmployeePanel` | Form with First Name, Last Name, IC/Passport, auto-generate username/password |
| `ProfilePanel` | Editable fields for employee's own data, Save button |
| `FamilyDetailsPanel` | Table showing family members + Add/Remove buttons |
| `LeaveManagementPanel` | Leave type dropdown, date pickers, reason textarea, Submit button |
| `ReportPanel` | Employee ID + Year inputs, Generate PDF/Print button |

**Swing best practices to follow:**
- Run all GUI updates on the **Event Dispatch Thread (EDT)** via `SwingUtilities.invokeLater()`
- Make all RMI calls from a **SwingWorker** background thread to keep the UI responsive
- Use `JTable` with a custom `DefaultTableModel` for displaying lists (employees, leave history)
- Use `GridBagLayout` or `MigLayout` for clean form layouts

---

## Security Implementation

```

```

1. **Authentication:** Verify username + BCrypt password hash on every `login()` call; return an `Employee` object or throw `RemoteException("Invalid credentials")`
2. **Authorization:** Check `employee.getRole()` before executing HR-only methods in `HRMServiceImpl`
3. **SQL Injection Prevention:** Use **PreparedStatement** exclusively — never concatenate user input into SQL strings

---

## Multi-threading Strategy

| Concern | Approach |
|---|---|
| Multiple clients calling RMI simultaneously | RMI handles each call in a separate thread automatically |
| Shared DB connection | Use HikariCP connection pool; each thread gets its own connection |
| Swing UI updates from background | Always use `SwingWorker` for RMI calls; update UI in `done()` |
| Synchronized report generation | `synchronized` keyword on `generateYearlyReport()` to avoid conflicts |

---

## Fault Tolerance

- Wrap all client-side RMI calls in try-catch for `RemoteException` and `ConnectException`
- Show a retry dialog if the server is unreachable: "Cannot connect to server. Retry?"
- Server should catch and log all exceptions without crashing (`try-catch` in every remote method)
- Use MySQL transactions (`conn.setAutoCommit(false)`) for multi-step operations — rollback on failure
- Log all server errors to a file (`java.util.logging` or SLF4J + Logback)

---

## Dependencies (Add to pom.xml or lib/ folder)

| Library | Purpose | Version |
|---|---|---|
| `mysql-connector-java` | JDBC driver for MySQL | 8.0.x |
| `HikariCP` | Database connection pool | 5.x |
| `bcrypt` (jBCrypt) | Password hashing | 0.4 |

---

## Notes for the AI Agent / Developer

- Never hardcode credentials; use a `config.properties` file for DB URL, username, password
- All RMI remote methods must declare `throws RemoteException` in the interface
- `serialVersionUID` must be defined in all Serializable classes or Java will warn
- Test with two separate machines (or two JVMs) to prove true distributed behaviour
- Use `JOptionPane.showMessageDialog()` for all user-facing errors on the client side
- Format the yearly report as a printable HTML or PDF using iText7