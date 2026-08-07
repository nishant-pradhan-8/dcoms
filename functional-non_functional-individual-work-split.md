## Functional Requirements


### Authentication & Session

| ID | Requirement |
|----|-------------|
| FR1 | Users can log in with a username and password, and are directed to role-appropriate functions (HR or Employee) upon successful login. |
| FR2 | Users can log out and a different user can log in without restarting the application. |
| FR3 | Employees can change their own password. |

### HR Functions

| ID | Requirement |
|----|-------------|
| FR4 | HR can register a new employee with First Name, Last Name, and IC/Passport number. |
| FR5 | HR can view a list of all registered employees. |
| FR6 | HR can edit an employee's identity details (First Name, Last Name, IC/Passport). |
| FR7 | HR can view and act on (approve or reject) pending leave applications. |
| FR8 | HR can generate a yearly report for an employee, covering profile, family details, and leave history. |

### Employee Functions

| ID | Requirement |
|----|-------------|
| FR9 | Employees can view their identity details (read-only) and update their own contact information (phone, email, address). |
| FR10 | Employees can view and add family member details (name, relationship, date of birth). |
| FR11 | Employees can view their current leave balance. |
| FR12 | Employees can apply for leave, subject to the following business rules: the start date cannot be in the past, the end date cannot precede the start date, and the application is rejected if the leave balance is insufficient. |
| FR13 | Employees can view the history and status of their own leave applications. |

---

##  Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR1 – Distribution | The system is implemented as a distributed client-server application using Java RMI. |
| NFR2 – Security | Data in transit is encrypted, and credentials are never stored or transmitted in plaintext. |
| NFR3 – Fault Tolerance | The client handles server-side and connectivity failures gracefully rather than crashing, offering the user a way to recover (e.g., reconnect). |
| NFR4 – Usability | The interface is understandable and operable by non-technical HR staff and employees. |
| NFR5 – Maintainability | The codebase is organized by concern, so components can be modified independently without wide-reaching changes. |
| NFR6 – Heterogeneity | The system can run across client and server machines on different platforms. |
| NFR7 – Concurrency | Multiple users can use the system simultaneously without data corruption or race conditions. |
| NFR8 – Scalability | The system can accommodate a growing number of employees and clients without architectural redesign. |

---

## Section 7 (Individual Report) — Implementation Detail


| Requirement | Implementation |
|---|---|
| FR1 | `login()` verifies credentials with BCrypt; returns role via `Employee` object; HR routed to `HRDashboard`, Employee to `EmployeeDashboard` |
| FR3 | `changePassword()` validates current password before storing new BCrypt hash |
| FR4 | `registerEmployee()`; username/password auto-generated server-side |
| FR6 | `updateEmployeeIdentity()` |
| FR7 | `getPendingLeaves()` / `updateLeaveStatus()`; approval conditionally deducts ANNUAL/SICK balance |
| FR8 | Yearly report aggregates profile, family, and leave history into HTML output |
| FR9 | `updateProfile()` for contact fields; identity fields rendered read-only client-side |
| FR10 | `getFamilyDetails()` / `addFamilyDetail()` |
| FR11 | `getLeaveBalance()` |
| FR12 | `applyLeave()`; date range validated client-side (date picker limits) and server-side; balance check via conditional SQL `UPDATE ... WHERE balance >= days` |
| FR13 | `getLeaveHistory()` |
| NFR2 | `SslRMIClientSocketFactory` / `SslRMIServerSocketFactory` (JSSE) for encrypted RMI transport; BCrypt for password hashing |
| NFR3 | `RemoteException` handling on all remote calls; `SwingWorker` for async client calls; Reconnect/Quit dialog on connection failure |
| NFR5 | Package separation: `common` (models/interface), `server` (RMI + DB), `client` (GUI); externalized config via `config.properties` |
| NFR6 | Pure JVM-based RMI; no OS-specific dependencies |
| NFR7 | `synchronized` blocks on `applyLeave()`/`updateLeaveStatus()`; HikariCP connection pooling; atomic SQL updates for leave balances |
| NFR8 | Centralized MySQL; connection pooling; host/port externalized in config |


## Individual Division

### Member 1 — Authentication, Session & Security
**Implements / writes about:**
- FR1 (login, role routing), FR2 (logout/switch user), FR3 (change password)
- NFR2 (SSL/TLS RMI, BCrypt hashing)
- NFR3 (fault tolerance — reconnect/quit on connection failure)

**Why this grouping works:** This is the security-and-connectivity backbone — login, encrypted RMI transport, and graceful failure handling are naturally one story (how a user gets in, stays secure, and what happens when things go wrong).

---

### Member 2 — HR Administration Module
**Implements / writes about:**
- FR4 (register employee), FR5 (view all employees), FR6 (edit identity), FR8 (yearly report generation)
- NFR5 (maintainability — package structure, config separation) — good fit since this member touches the most cross-cutting admin code

**Why this grouping works:** This is everything HR does *except* leave approval — registration, employee records, and reporting form a coherent "HR back-office" story.

---

### Member 3 — Employee Self-Service (Profile & Family)
**Implements / writes about:**
- FR9 (view identity read-only, update contact info), FR10 (family member add/view)
- NFR4 (usability — Swing GUI design, tabbed dashboards, validation feedback)

**Why this grouping works:** This is the employee-facing data management side — natural pairing with usability, since this module has the most form-based UI/UX decisions to justify.

---

### Member 4 — Leave Management Module
**Implements / writes about:**
- FR7 (HR approve/reject leave), FR11 (view leave balance), FR12 (apply for leave + business rules), FR13 (leave history)
- NFR7 (concurrency — `synchronized` blocks, atomic SQL balance updates)
- NFR6 (heterogeneity) and NFR8 (scalability) — assign whichever of these two is thinner elsewhere, or split between Members 2 and 4

**Why this grouping works:** Leave application is the most complex business-logic module (multi-step: apply → validate → approve → balance deduction) and pairs naturally with concurrency, since leave balance updates are exactly where race conditions could occur.

---

## For Task 9 (Testing)

Each member writes the testing manual entries **for the module they implemented**:

| Member | Test cases to write |
|---|---|
| Member 1 | Login (valid/invalid credentials), logout/re-login, password change, connection failure/reconnect |
| Member 2 | Employee registration, view employee list, edit identity, generate yearly report |
| Member 3 | Update contact info, add family member, view identity (confirm read-only) |
| Member 4 | Apply for leave (valid/invalid dates, insufficient balance), approve/reject leave, view balance/history, concurrent leave application test |
