-- HRM System database schema (MySQL)
-- Re-runnable: drops existing tables before recreating them.

CREATE DATABASE IF NOT EXISTS hrm_db;
USE hrm_db;

-- Drop child tables first (foreign key dependencies)
DROP TABLE IF EXISTS leave_applications;
DROP TABLE IF EXISTS leave_balance;
DROP TABLE IF EXISTS family_details;
DROP TABLE IF EXISTS employees;

CREATE TABLE employees (
    emp_id        INT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    ic_passport   VARCHAR(50)  NOT NULL UNIQUE,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('HR', 'EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE',
    phone_number  VARCHAR(20),
    email         VARCHAR(100),
    address       VARCHAR(255),
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

    FOREIGN KEY (emp_id)
        REFERENCES employees(emp_id)
        ON DELETE CASCADE,

    FOREIGN KEY (approved_by)
        REFERENCES employees(emp_id)
        ON DELETE SET NULL
);

-- Default HR admin (password: admin123)
INSERT INTO employees (first_name, last_name, ic_passport, username, password_hash, role)
VALUES (
    'System',
    'Administrator',
    'ADMIN001',
    'admin',
    '$2a$10$Re5mlW2yKG9Sam5B2t7VJu1g7olystmQ.2sui911DdxrDBXGJWYRu',
    'HR'
);

INSERT INTO leave_balance (emp_id, annual_leave, sick_leave)
VALUES (LAST_INSERT_ID(), 14, 14);
