# HRM System — Setup Guide

Distributed **Human Resource Management** system built with:

- **Java RMI** (SSL/TLS-secured) for client–server communication  
- **Java Swing** for the GUI client  
- **MySQL** for the database  

---

## Functional requirements

Requirements the system implements today.

### Authentication & session

| ID | Requirement | How it is met |
|----|-------------|----------------|
| FR1 | Users can log in with username and password | `login()` verifies credentials with BCrypt; role returned in `Employee` |
| FR2 | Users are routed by role after login | HR → `HRDashboard`; Employee → `EmployeeDashboard` |
| FR3 | Users can log out and switch accounts without restarting the app | Logout closes the dashboard and reopens `LoginFrame` |
| FR4 | Employees can change their password | `changePassword()` checks current password, stores new BCrypt hash |

### HR functions

| ID | Requirement | How it is met |
|----|-------------|----------------|
| FR5 | HR can register a new employee | First name, last name, IC/passport; username/password auto-generated |
| FR6 | HR can view all employees | `getAllEmployees()` shown in a table |
| FR7 | HR can edit employee identity | First name, last name, IC/passport via `updateEmployeeIdentity()` |
| FR8 | HR can view pending leave applications | `getPendingLeaves()` on Approve Leave tab |
| FR9 | HR can approve or reject leave | `updateLeaveStatus()`; approval deducts ANNUAL/SICK balance |
| FR10 | HR can generate a yearly employee report | Profile + family + leave history for a given year (HTML) |

### Employee functions

| ID | Requirement | How it is met |
|----|-------------|----------------|
| FR11 | Employee can view identity fields (read-only) | First name, last name, IC/passport shown but not editable |
| FR12 | Employee can update contact details | Phone, email, address via `updateProfile()` |
| FR13 | Employee can view and add family members | `getFamilyDetails()` / `addFamilyDetail()` |
| FR14 | Employee can view leave balance | Annual and sick leave via `getLeaveBalance()` |
| FR15 | Employee can apply for leave | Leave type, date pickers, reason via `applyLeave()` |
| FR16 | Employee can view own leave history | `getLeaveHistory()` in a table |

### Leave rules (business constraints)

| ID | Requirement | How it is met |
|----|-------------|----------------|
| FR17 | Leave start date cannot be before today | Date picker range limit + submit validation |
| FR18 | Leave end date cannot be before start date | Client and server validation |
| FR19 | Insufficient leave balance blocks approval | Conditional SQL `UPDATE ... WHERE balance >= days` |
| FR20 | New employees get default leave balances | On registration: annual 14, sick 14 |

---

## Non-functional requirements

| ID | Requirement | How it is met |
|----|-------------|----------------|
| **NFR1 – Distribution** | Client and server communicate as a distributed system | Java RMI: `HRMService` remote interface; server binds/exports `HRMServiceImpl`; client looks up the stub |
| **NFR2 – Security** | Sensitive data must not travel in plaintext; credentials protected | SSL/TLS RMI via `SslRMIClientSocketFactory` / `SslRMIServerSocketFactory` (JSSE); BCrypt password hashing; role-based UI (HR vs Employee) |
| **NFR3 – Fault tolerance** | Failures handled without crashing the client abruptly | Remote methods wrap errors in `RemoteException`; client uses `SwingWorker` + dialogs; startup **Reconnect / Quit** if the server is unreachable |
| **NFR4 – Usability** | Interface usable by non-technical HR and employees | Swing GUI, tabbed dashboards, labels above fields, date pickers, confirmation/error dialogs, logout |
| **NFR5 – Maintainability** | Code separated by concern | Packages: `common` (models/interface), `server` (RMI + DB), `client` (GUI); config in `config.properties`; SQL in `DatabaseManager` |
| **NFR6 – Heterogeneity** | Clients/servers can run on different platforms | JVM-based Java RMI; no native/OS-specific UI or networking code |
| **NFR7 – Concurrency** | Multiple users can access the system at once safely | RMI multi-threaded calls; `synchronized` on `applyLeave` / `updateLeaveStatus`; HikariCP pool; atomic leave-balance SQL |
| **NFR8 – Scalability** | More employees/clients can be added without redesign | Centralized MySQL; connection pooling; config-driven host/port; registration creates new employees without code changes |

### NFR notes (honest scope)

- **NFR2:** Transport encryption uses **server authentication** (keystore + client truststore). Application login is separate (username/password). This is not mutual TLS with client certificates.
- **NFR3:** Startup reconnect is supported; mid-session automatic reconnect after a server restart is limited (user may need to restart the client or log out/in).
- **NFR5:** Logical modules exist (packages + `DatabaseManager`), but remote APIs are exposed through a single `HRMService` interface rather than separate employee/leave/report remote services.
- **NFR8:** Suitable for multi-client coursework use; not a clustered/high-availability multi-server deployment.

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|--------|
| **Java JDK** | 17+ | `java -version` and `javac -version` |
| **MySQL** | 8.x | Running locally (or reachable host) |
| **keytool** | Bundled with JDK | Used to generate SSL certificates |

---

## 1. Clone / copy the project

```bash
cd /path/to/DCOMS
```

Work from the **project root** for all commands below (so `config.properties` and `ssl/` resolve correctly).

---

## 2. Download dependencies (`lib/`)

The `lib/` folder is not committed. Create it and download the JARs:

```bash
mkdir -p lib
cd lib

curl -fsSL -o HikariCP-5.1.0.jar \
  "https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"

curl -fsSL -o jbcrypt-0.4.jar \
  "https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar"

curl -fsSL -o mysql-connector-j-8.3.0.jar \
  "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar"

curl -fsSL -o slf4j-api-2.0.13.jar \
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar"

curl -fsSL -o slf4j-simple-2.0.13.jar \
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar"

curl -fsSL -o LGoodDatePicker-11.2.1.jar \
  "https://repo1.maven.org/maven2/com/github/lgooddatepicker/LGoodDatePicker/11.2.1/LGoodDatePicker-11.2.1.jar"

cd ..
ls lib
```

Expected JARs:

- `HikariCP-5.1.0.jar`
- `jbcrypt-0.4.jar`
- `mysql-connector-j-8.3.0.jar`
- `slf4j-api-2.0.13.jar`
- `slf4j-simple-2.0.13.jar`
- `LGoodDatePicker-11.2.1.jar`

---

## 3. Configure the database

### Create schema + seed admin user

```bash
mysql -u root -p < db/schema.sql
```

This creates database `hrm_db`, all tables, and a default HR admin:

| Field | Value |
|-------|--------|
| Username | `admin` |
| Password | `admin123` |
| Role | HR |

> Re-running `db/schema.sql` **drops and recreates** tables (data is wiped).

---

## 4. Create `config.properties`

```bash
cp config.properties.example config.properties
```

Edit `config.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/hrm_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=YOUR_MYSQL_PASSWORD
rmi.host=localhost
rmi.port=1099

ssl.keystore=ssl/server-keystore.jks
ssl.keystore.password=changeit
ssl.truststore=ssl/client-truststore.jks
ssl.truststore.password=changeit
```

| Property | Purpose |
|----------|---------|
| `db.*` | MySQL connection (used by the server) |
| `rmi.host` / `rmi.port` | RMI bind/lookup address |
| `ssl.*` | Keystore (server) and truststore (client) |

For a remote server machine, set `rmi.host` to that machine’s IP/hostname on **both** server and client configs.

---

## 5. Generate SSL certificates

RMI communication is wrapped with SSL/TLS. Generate a keystore and truststore:

```bash
bash ssl/generate-certs.sh
```

This creates:

- `ssl/server-keystore.jks` — used by the server  
- `ssl/client-truststore.jks` — used by the client  
- `ssl/server.cer` — exported certificate  

Default store password: `changeit` (dev only).

---

## 6. Compile

```bash
mkdir -p out

# Shared models + remote interface
javac -d out src/common/*.java

# Server
javac -cp "lib/*:out" -d out src/server/*.java

# Client
javac -cp "lib/*:out" -d out src/client/*.java
```

**macOS / Linux:** use `lib/*:out`  
**Windows (cmd):** use `lib/*;out`

---

## 7. Run

### Terminal 1 — Server

```bash
java -cp "lib/*:out" server.HRMServer
```

You should see something like:

```text
HRM Server started with SSL/TLS. Service bound as 'HRMService' at rmi://localhost:1099/HRMService
```

### Terminal 2 — Client

```bash
java -cp "lib/*:out" client.HRMClient
```

If the server is down, the client shows a **Reconnect / Quit** dialog instead of exiting immediately.

---

## 8. First login

1. Open the client login window  
2. Sign in as **admin** / **admin123** (HR dashboard)  
3. Use **Register Employee** to create employees (username/password are auto-generated)  
4. **Logout**, then log in with the employee credentials  

---

## Project structure

```text
DCOMS/
├── config.properties.example
├── db/
│   └── schema.sql
├── lib/                    # JARs (download; not in git)
├── ssl/
│   └── generate-certs.sh
├── src/
│   ├── common/             # Shared RMI interface + models
│   ├── server/             # RMI server + DB access
│   └── client/             # Swing GUI
└── out/                    # Compiled classes
```

---

## Features overview

| Role | Capabilities |
|------|----------------|
| **HR** | Register employees, view employees, edit identity (name/IC), approve/reject leave, yearly HTML reports |
| **Employee** | Update phone/email/address, change password, family details, leave balance, apply leave, leave history |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `SSL keystore not found` / `truststore not found` | Run `bash ssl/generate-certs.sh` from project root |
| `Cannot connect to SSL RMI server` | Start the server first; check `rmi.host` / `rmi.port` |
| MySQL connection error | Check MySQL is running and `db.password` in `config.properties` |
| `package com.zaxxer.hikari does not exist` | Ensure JARs are in `lib/` and classpath includes `lib/*` |
| `Invalid salt revision` on admin login | Re-run `db/schema.sql` (admin hash must be `$2a$` for jBCrypt) |
| Client compiles but date picker fails | Include `LGoodDatePicker-11.2.1.jar` and compile client with `lib/*:out` |
| Port 1099 already in use | Stop the other RMI process, or change `rmi.port` in config |

---

## Quick reference

```bash
# Setup (once)
cp config.properties.example config.properties   # then edit password
mysql -u root -p < db/schema.sql
bash ssl/generate-certs.sh
# download lib/*.jar (see section 2)

# Build
javac -d out src/common/*.java
javac -cp "lib/*:out" -d out src/server/*.java
javac -cp "lib/*:out" -d out src/client/*.java

# Run
java -cp "lib/*:out" server.HRMServer
java -cp "lib/*:out" client.HRMClient
```
