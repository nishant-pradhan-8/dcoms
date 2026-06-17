package server;

import common.Employee;
import common.FamilyDetail;
import common.HRMService;
import common.LeaveApplication;
import org.mindrot.jbcrypt.BCrypt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HRMServiceImpl extends UnicastRemoteObject implements HRMService {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(HRMServiceImpl.class.getName());

    private final DatabaseManager db;

    public HRMServiceImpl() throws RemoteException {
        super();
        this.db = DatabaseManager.getInstance();
    }

    @Override
    public String registerEmployee(String firstName, String lastName, String icPassport,
                                   String username, String password) throws RemoteException {
        try {
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            int empId = db.insertEmployee(firstName, lastName, icPassport, username, passwordHash, "EMPLOYEE");
            return "Employee registered successfully. ID: " + empId + ", Username: " + username;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to register employee", e);
            throw new RemoteException("Failed to register employee: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during employee registration", e);
            throw new RemoteException("Failed to register employee: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Employee> getAllEmployees() throws RemoteException {
        try {
            return db.getAllEmployees();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve employees", e);
            throw new RemoteException("Failed to retrieve employees: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving employees", e);
            throw new RemoteException("Failed to retrieve employees: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateYearlyReport(int empId, int year) throws RemoteException {
        try {
            Employee employee = db.getAllEmployees().stream()
                    .filter(emp -> emp.getEmpId() == empId)
                    .findFirst()
                    .orElse(null);

            if (employee == null) {
                throw new RemoteException("Employee not found for ID: " + empId);
            }

            List<FamilyDetail> familyDetails = db.getFamilyDetailsByEmpId(empId);
            List<LeaveApplication> leaves = db.getLeaveReportByYear(empId, year);

            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Yearly Report ").append(year).append("</title></head><body>");
            html.append("<h1>Yearly Employee Report</h1>");
            html.append("<p>Year: ").append(year).append("</p>");

            html.append("<h2>Employee Profile</h2>");
            html.append("<p>Employee ID: ").append(employee.getEmpId()).append("</p>");
            html.append("<p>First Name: ").append(escapeHtml(employee.getFirstName())).append("</p>");
            html.append("<p>Last Name: ").append(escapeHtml(employee.getLastName())).append("</p>");
            html.append("<p>IC/Passport: ").append(escapeHtml(employee.getIcPassport())).append("</p>");
            html.append("<p>Username: ").append(escapeHtml(employee.getUsername())).append("</p>");
            html.append("<p>Role: ").append(escapeHtml(employee.getRole())).append("</p>");

            html.append("<h2>Family Details</h2>");
            html.append("<table border='1' cellpadding='5'>");
            html.append("<tr><th>Member Name</th><th>Relationship</th><th>Date of Birth</th></tr>");
            if (familyDetails.isEmpty()) {
                html.append("<tr><td colspan='3'>No family members recorded.</td></tr>");
            } else {
                for (FamilyDetail detail : familyDetails) {
                    html.append("<tr>")
                            .append("<td>").append(escapeHtml(detail.getMemberName())).append("</td>")
                            .append("<td>").append(escapeHtml(detail.getRelationship())).append("</td>")
                            .append("<td>").append(detail.getDob()).append("</td>")
                            .append("</tr>");
                }
            }
            html.append("</table>");

            html.append("<h2>Leave History (").append(year).append(")</h2>");
            html.append("<table border='1' cellpadding='5'>");
            html.append("<tr><th>Leave ID</th><th>Type</th><th>Start</th><th>End</th>")
                    .append("<th>Status</th><th>Reason</th><th>Applied On</th></tr>");
            if (leaves.isEmpty()) {
                html.append("<tr><td colspan='7'>No leave applications for this year.</td></tr>");
            } else {
                for (LeaveApplication leave : leaves) {
                    html.append("<tr>")
                            .append("<td>").append(leave.getLeaveId()).append("</td>")
                            .append("<td>").append(leave.getLeaveType()).append("</td>")
                            .append("<td>").append(leave.getStartDate()).append("</td>")
                            .append("<td>").append(leave.getEndDate()).append("</td>")
                            .append("<td>").append(leave.getStatus()).append("</td>")
                            .append("<td>").append(escapeHtml(leave.getReason())).append("</td>")
                            .append("<td>").append(leave.getAppliedOn()).append("</td>")
                            .append("</tr>");
                }
            }

            html.append("</table></body></html>");
            return html.toString().getBytes();
        } catch (RemoteException e) {
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate yearly report", e);
            throw new RemoteException("Failed to generate yearly report: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error generating yearly report", e);
            throw new RemoteException("Failed to generate yearly report: " + e.getMessage(), e);
        }
    }

    @Override
    public Employee login(String username, String password) throws RemoteException {
        try {
            Employee employee = db.getEmployeeByUsername(username);
            String passwordHash = db.getPasswordHashByUsername(username);

            if (employee == null || passwordHash == null || !BCrypt.checkpw(password, passwordHash)) {
                throw new RemoteException("Invalid credentials");
            }

            return employee;
        } catch (RemoteException e) {
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to authenticate user: " + username, e);
            throw new RemoteException("Failed to authenticate user: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during login", e);
            throw new RemoteException("Failed to authenticate user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateProfile(int empId, Employee updatedData) throws RemoteException {
        try {
            return db.updateEmployeeProfile(empId, updatedData);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update profile for empId: " + empId, e);
            throw new RemoteException("Failed to update profile: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating profile", e);
            throw new RemoteException("Failed to update profile: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean addFamilyDetail(int empId, FamilyDetail detail) throws RemoteException {
        try {
            return db.insertFamilyDetail(empId, detail);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add family detail for empId: " + empId, e);
            throw new RemoteException("Failed to add family detail: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error adding family detail", e);
            throw new RemoteException("Failed to add family detail: " + e.getMessage(), e);
        }
    }

    @Override
    public List<FamilyDetail> getFamilyDetails(int empId) throws RemoteException {
        try {
            return db.getFamilyDetailsByEmpId(empId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get family details for empId: " + empId, e);
            throw new RemoteException("Failed to get family details: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving family details", e);
            throw new RemoteException("Failed to get family details: " + e.getMessage(), e);
        }
    }

    @Override
    public int getLeaveBalance(int empId, String leaveType) throws RemoteException {
        try {
            return db.getLeaveBalance(empId, leaveType);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get leave balance for empId: " + empId, e);
            throw new RemoteException("Failed to get leave balance: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving leave balance", e);
            throw new RemoteException("Failed to get leave balance: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized String applyLeave(int empId, LeaveApplication application) throws RemoteException {
        try {
            application.setEmpId(empId);
            application.setStatus("PENDING");

            validateLeaveDates(application);

            int requestedDays = calculateLeaveDays(application);
            String leaveType = application.getLeaveType();

            if ("ANNUAL".equalsIgnoreCase(leaveType) || "SICK".equalsIgnoreCase(leaveType)) {
                int balance = db.getLeaveBalance(empId, leaveType);
                if (requestedDays > balance) {
                    throw new RemoteException("Insufficient " + leaveType.toLowerCase()
                            + " leave balance. Requested: " + requestedDays + ", Available: " + balance);
                }
            }

            int leaveId = db.insertLeaveApplication(application);
            return "Leave application submitted successfully. Reference ID: " + leaveId;
        } catch (RemoteException e) {
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to apply leave for empId: " + empId, e);
            throw new RemoteException("Failed to apply leave: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error applying leave", e);
            throw new RemoteException("Failed to apply leave: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LeaveApplication> getLeaveHistory(int empId) throws RemoteException {
        try {
            return db.getLeaveHistoryByEmpId(empId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get leave history for empId: " + empId, e);
            throw new RemoteException("Failed to get leave history: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving leave history", e);
            throw new RemoteException("Failed to get leave history: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LeaveApplication> getPendingLeaves() throws RemoteException {
        try {
            return db.getPendingLeaves();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get pending leaves", e);
            throw new RemoteException("Failed to get pending leaves: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error retrieving pending leaves", e);
            throw new RemoteException("Failed to get pending leaves: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized boolean updateLeaveStatus(int leaveId, String status) throws RemoteException {
        try {
            if ("APPROVED".equalsIgnoreCase(status)) {
                LeaveApplication leave = findPendingLeave(leaveId);
                if (leave == null) {
                    throw new RemoteException("Pending leave application not found for ID: " + leaveId);
                }

                int days = calculateLeaveDays(leave);
                String leaveType = leave.getLeaveType();

                if ("ANNUAL".equalsIgnoreCase(leaveType) || "SICK".equalsIgnoreCase(leaveType)) {
                    if (!db.deductLeaveBalance(leave.getEmpId(), leaveType, days)) {
                        throw new RemoteException("Failed to deduct leave balance. Insufficient balance for "
                                + leaveType.toLowerCase() + " leave.");
                    }
                }
            }

            return db.updateLeaveStatus(leaveId, status.toUpperCase());
        } catch (RemoteException e) {
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update leave status for leaveId: " + leaveId, e);
            throw new RemoteException("Failed to update leave status: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating leave status", e);
            throw new RemoteException("Failed to update leave status: " + e.getMessage(), e);
        }
    }

    private LeaveApplication findPendingLeave(int leaveId) throws SQLException {
        for (LeaveApplication leave : db.getPendingLeaves()) {
            if (leave.getLeaveId() == leaveId) {
                return leave;
            }
        }
        return null;
    }

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
                application.getEndDate().toLocalDate()
        ) + 1;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
