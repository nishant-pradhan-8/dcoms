package server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import common.Employee;
import common.FamilyDetail;
import common.LeaveApplication;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;

    private final HikariDataSource dataSource;

    private DatabaseManager() {
        Properties props = loadConfig();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public int insertEmployee(String firstName, String lastName, String icPassport,
                              String username, String passwordHash, String role) throws SQLException {
        String employeeSql = """
                INSERT INTO employees (first_name, last_name, ic_passport, username, password_hash, role)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        String balanceSql = """
                INSERT INTO leave_balance (emp_id, annual_leave, sick_leave)
                VALUES (?, 14, 14)
                """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int empId;
                try (PreparedStatement ps = conn.prepareStatement(employeeSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, firstName);
                    ps.setString(2, lastName);
                    ps.setString(3, icPassport);
                    ps.setString(4, username);
                    ps.setString(5, passwordHash);
                    ps.setString(6, role);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to retrieve generated employee ID.");
                        }
                        empId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(balanceSql)) {
                    ps.setInt(1, empId);
                    ps.executeUpdate();
                }

                conn.commit();
                return empId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert employee", e);
            throw e;
        }
    }

    public Employee getEmployeeByUsername(String username) throws SQLException {
        String sql = """
                SELECT emp_id, first_name, last_name, ic_passport, username, role,
                       phone_number, email, address
                FROM employees
                WHERE username = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapEmployee(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get employee by username: " + username, e);
            throw e;
        }
    }

    public Employee getEmployeeById(int empId) throws SQLException {
        String sql = """
                SELECT emp_id, first_name, last_name, ic_passport, username, role,
                       phone_number, email, address
                FROM employees
                WHERE emp_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapEmployee(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get employee by empId: " + empId, e);
            throw e;
        }
    }

    public String getPasswordHashByUsername(String username) throws SQLException {
        String sql = "SELECT password_hash FROM employees WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
                return null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get password hash for username: " + username, e);
            throw e;
        }
    }

    public String getPasswordHashByEmpId(int empId) throws SQLException {
        String sql = "SELECT password_hash FROM employees WHERE emp_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
                return null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get password hash for empId: " + empId, e);
            throw e;
        }
    }

    public boolean updatePasswordHash(int empId, String passwordHash) throws SQLException {
        String sql = "UPDATE employees SET password_hash = ? WHERE emp_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, empId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update password for empId: " + empId, e);
            throw e;
        }
    }

    public List<Employee> getAllEmployees() throws SQLException {
        String sql = """
                SELECT emp_id, first_name, last_name, ic_passport, username, role,
                       phone_number, email, address
                FROM employees
                ORDER BY emp_id
                """;

        List<Employee> employees = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employees.add(mapEmployee(rs));
            }
            return employees;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get all employees", e);
            throw e;
        }
    }

    public boolean insertFamilyDetail(int empId, FamilyDetail detail) throws SQLException {
        String sql = """
                INSERT INTO family_details (emp_id, member_name, relationship, dob)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setString(2, detail.getMemberName());
            ps.setString(3, detail.getRelationship());
            ps.setDate(4, detail.getDob());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert family detail for empId: " + empId, e);
            throw e;
        }
    }

    public boolean deleteFamilyDetail(int empId, int detailId) throws SQLException {
        String sql = """
                DELETE FROM family_details
                WHERE detail_id = ? AND emp_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, detailId);
            ps.setInt(2, empId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to delete family detail " + detailId + " for empId: " + empId, e);
            throw e;
        }
    }

    public List<FamilyDetail> getFamilyDetailsByEmpId(int empId) throws SQLException {
        String sql = """
                SELECT detail_id, emp_id, member_name, relationship, dob
                FROM family_details
                WHERE emp_id = ?
                ORDER BY detail_id
                """;

        List<FamilyDetail> details = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.add(mapFamilyDetail(rs));
                }
            }
            return details;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get family details for empId: " + empId, e);
            throw e;
        }
    }

    public int getLeaveBalance(int empId, String leaveType) throws SQLException {
        String column = resolveLeaveBalanceColumn(leaveType);
        if (column == null) {
            return 0;
        }

        String sql = "SELECT " + column + " FROM leave_balance WHERE emp_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get leave balance for empId: " + empId, e);
            throw e;
        }
    }

    public int insertLeaveApplication(LeaveApplication application) throws SQLException {
        String sql = """
                INSERT INTO leave_applications (emp_id, leave_type, start_date, end_date, reason, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, application.getEmpId());
            ps.setString(2, application.getLeaveType());
            ps.setDate(3, application.getStartDate());
            ps.setDate(4, application.getEndDate());
            ps.setString(5, application.getReason());
            ps.setString(6, application.getStatus() != null ? application.getStatus() : "PENDING");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Failed to retrieve generated leave application ID.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert leave application", e);
            throw e;
        }
    }

    /**
     * Atomically approve or reject a PENDING leave. Deducts ANNUAL/SICK balance on approve
     * in the same transaction. Returns false if the leave is missing or not PENDING.
     */
    public boolean updatePendingLeaveStatus(int leaveId, String status, int approvedBy)
            throws SQLException {
        String selectSql = """
                SELECT leave_id, emp_id, leave_type, start_date, end_date, reason, status,
                       applied_on, approved_by, approved_at
                FROM leave_applications
                WHERE leave_id = ? AND status = 'PENDING'
                FOR UPDATE
                """;
        String updateSql = """
                UPDATE leave_applications
                SET status = ?,
                    approved_by = ?,
                    approved_at = CURRENT_TIMESTAMP
                WHERE leave_id = ? AND status = 'PENDING'
                """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LeaveApplication leave;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, leaveId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        leave = mapLeaveApplication(rs);
                    }
                }

                if ("APPROVED".equals(status)) {
                    String leaveType = leave.getLeaveType();
                    if ("ANNUAL".equalsIgnoreCase(leaveType) || "SICK".equalsIgnoreCase(leaveType)) {
                        int days = calculateLeaveDays(leave);
                        if (!deductLeaveBalance(conn, leave.getEmpId(), leaveType, days)) {
                            conn.rollback();
                            throw new SQLException("Insufficient " + leaveType.toLowerCase()
                                    + " leave balance for leave ID: " + leaveId);
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, status);
                    ps.setInt(2, approvedBy);
                    ps.setInt(3, leaveId);
                    if (ps.executeUpdate() != 1) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update leave status for leaveId: " + leaveId, e);
            throw e;
        }
    }

    private boolean deductLeaveBalance(Connection conn, int empId, String leaveType, int days)
            throws SQLException {
        String column = resolveLeaveBalanceColumn(leaveType);
        if (column == null) {
            return true;
        }

        String sql = """
                UPDATE leave_balance
                SET %s = %s - ?
                WHERE emp_id = ? AND %s >= ?
                """.formatted(column, column, column);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            ps.setInt(2, empId);
            ps.setInt(3, days);
            return ps.executeUpdate() == 1;
        }
    }

    private int calculateLeaveDays(LeaveApplication application) {
        return (int) ChronoUnit.DAYS.between(
                application.getStartDate().toLocalDate(),
                application.getEndDate().toLocalDate()
        ) + 1;
    }

    public List<LeaveApplication> getLeaveHistoryByEmpId(int empId) throws SQLException {
        String sql = """
                SELECT leave_id, emp_id, leave_type, start_date, end_date, reason, status,
                       applied_on, approved_by, approved_at
                FROM leave_applications
                WHERE emp_id = ?
                ORDER BY applied_on DESC, leave_id DESC
                """;

        return queryLeaveApplications(sql, ps -> ps.setInt(1, empId));
    }

    public List<LeaveApplication> getPendingLeaves() throws SQLException {
        String sql = """
                SELECT leave_id, emp_id, leave_type, start_date, end_date, reason, status,
                       applied_on, approved_by, approved_at
                FROM leave_applications
                WHERE status = 'PENDING'
                ORDER BY applied_on ASC, leave_id ASC
                """;

        return queryLeaveApplications(sql, ps -> { });
    }

    public boolean updateEmployeeProfile(int empId, Employee updatedData) throws SQLException {
        String sql = """
                UPDATE employees
                SET phone_number = ?, email = ?, address = ?
                WHERE emp_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updatedData.getPhoneNumber());
            ps.setString(2, updatedData.getEmail());
            ps.setString(3, updatedData.getAddress());
            ps.setInt(4, empId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update employee profile for empId: " + empId, e);
            throw e;
        }
    }

    public boolean updateEmployeeIdentity(int empId, String firstName, String lastName, String icPassport)
            throws SQLException {
        String sql = """
                UPDATE employees
                SET first_name = ?, last_name = ?, ic_passport = ?
                WHERE emp_id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, icPassport);
            ps.setInt(4, empId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update employee identity for empId: " + empId, e);
            throw e;
        }
    }

    public List<LeaveApplication> getLeaveReportByYear(int empId, int year) throws SQLException {
        String sql = """
                SELECT leave_id, emp_id, leave_type, start_date, end_date, reason, status,
                       applied_on, approved_by, approved_at
                FROM leave_applications
                WHERE emp_id = ?
                  AND (YEAR(start_date) = ? OR YEAR(end_date) = ?)
                ORDER BY start_date ASC, leave_id ASC
                """;

        return queryLeaveApplications(sql, ps -> {
            ps.setInt(1, empId);
            ps.setInt(2, year);
            ps.setInt(3, year);
        });
    }

    private List<LeaveApplication> queryLeaveApplications(String sql, StatementBinder binder)
            throws SQLException {
        List<LeaveApplication> applications = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    applications.add(mapLeaveApplication(rs));
                }
            }
            return applications;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to query leave applications", e);
            throw e;
        }
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("emp_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("ic_passport"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("phone_number"),
                rs.getString("email"),
                rs.getString("address")
        );
    }

    private FamilyDetail mapFamilyDetail(ResultSet rs) throws SQLException {
        return new FamilyDetail(
                rs.getInt("detail_id"),
                rs.getInt("emp_id"),
                rs.getString("member_name"),
                rs.getString("relationship"),
                rs.getDate("dob")
        );
    }

    private LeaveApplication mapLeaveApplication(ResultSet rs) throws SQLException {
        LeaveApplication application = new LeaveApplication();
        application.setLeaveId(rs.getInt("leave_id"));
        application.setEmpId(rs.getInt("emp_id"));
        application.setLeaveType(rs.getString("leave_type"));
        application.setStartDate(rs.getDate("start_date"));
        application.setEndDate(rs.getDate("end_date"));
        application.setReason(rs.getString("reason"));
        application.setStatus(rs.getString("status"));
        application.setAppliedOn(toSqlDate(rs.getTimestamp("applied_on")));

        int approvedBy = rs.getInt("approved_by");
        if (rs.wasNull()) {
            application.setApprovedBy(0);
        } else {
            application.setApprovedBy(approvedBy);
        }

        application.setApprovedAt(toSqlDate(rs.getTimestamp("approved_at")));
        return application;
    }

    private Date toSqlDate(Timestamp timestamp) {
        return timestamp != null ? new Date(timestamp.getTime()) : null;
    }

    private String resolveLeaveBalanceColumn(String leaveType) {
        if (leaveType == null) {
            return null;
        }
        return switch (leaveType.toUpperCase()) {
            case "ANNUAL" -> "annual_leave";
            case "SICK" -> "sick_leave";
            default -> null;
        };
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load config.properties from classpath", e);
        }

        Path configPath = Path.of("config.properties");
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load config.properties", e);
            }
        } else {
            throw new IllegalStateException("config.properties not found on classpath or working directory");
        }
        return props;
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
