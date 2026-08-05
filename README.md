# HRM System — Setup Guide

Distributed **Human Resource Management** system built with:

- **Java RMI** (SSL/TLS-secured) for client–server communication  
- **Java Swing** for the GUI client  
- **MySQL** for the database  

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
