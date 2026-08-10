package server;

import common.Employee;
import common.FamilyDetail;
import common.HRMService;
import common.LeaveApplication;
import org.mindrot.jbcrypt.BCrypt;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
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

    public HRMServiceImpl(RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(0, csf, ssf);
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
    public boolean updateEmployeeIdentity(int empId, String firstName, String lastName,
                                          String icPassport) throws RemoteException {
        try {
            return db.updateEmployeeIdentity(empId, firstName, lastName, icPassport);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update employee identity for empId: " + empId, e);
            throw new RemoteException("Failed to update employee identity: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error updating employee identity", e);
            throw new RemoteException("Failed to update employee identity: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateYearlyReport(int empId, int year) throws RemoteException {
        try {
            Employee employee = db.getEmployeeById(empId);
            if (employee == null) {
                throw new RemoteException("Employee not found for ID: " + empId);
            }

            List<FamilyDetail> familyDetails = db.getFamilyDetailsByEmpId(empId);
            List<LeaveApplication> leaves = db.getLeaveReportByYear(empId, year);

            StringBuilder html = new StringBuilder();

            html.append("<html>");
            html.append("<head><title>Yearly Employee Report</title></head>");
            html.append("<body>");

            // Outer wrapper adds left/right spacing (Swing HTML has limited CSS support)
            html.append("<table border='0' width='100%' cellpadding='20' cellspacing='0'>");
            html.append("<tr><td>");

            html.append("<h1 align='center'>Yearly Employee Report</h1>");
            html.append("<h2 align='center'>Year: ").append(year).append("</h2>");
            html.append("<br>");

            /* =========================
             * Employee Profile
             * ========================= */
            html.append("<h2 align='center'>Employee Profile</h2>");
            html.append("<table border='1' cellpadding='8' cellspacing='0' width='100%'>");
            html.append("<tr><th align='center'>Field</th><th align='center'>Value</th></tr>");
            html.append("<tr><td align='center'><b>Employee ID</b></td><td align='center'>")
                    .append(employee.getEmpId()).append("</td></tr>");
            html.append("<tr><td align='center'><b>First Name</b></td><td align='center'>")
                    .append(escapeHtml(employee.getFirstName())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Last Name</b></td><td align='center'>")
                    .append(escapeHtml(employee.getLastName())).append("</td></tr>");
            html.append("<tr><td align='center'><b>IC / Passport</b></td><td align='center'>")
                    .append(escapeHtml(employee.getIcPassport())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Username</b></td><td align='center'>")
                    .append(escapeHtml(employee.getUsername())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Role</b></td><td align='center'>")
                    .append(escapeHtml(employee.getRole())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Phone</b></td><td align='center'>")
                    .append(escapeHtml(employee.getPhoneNumber())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Email</b></td><td align='center'>")
                    .append(escapeHtml(employee.getEmail())).append("</td></tr>");
            html.append("<tr><td align='center'><b>Address</b></td><td align='center'>")
                    .append(escapeHtml(employee.getAddress())).append("</td></tr>");
            html.append("</table>");

            html.append("<br><br>");

            /* =========================
             * Family Details
             * ========================= */
            html.append("<h2 align='center'>Family Details</h2>");
            html.append("<table border='1' cellpadding='8' cellspacing='0' width='100%'>");
            html.append("<tr>");
            html.append("<th align='center'>Member Name</th>");
            html.append("<th align='center'>Relationship</th>");
            html.append("<th align='center'>Date of Birth</th>");
            html.append("</tr>");

            if (familyDetails.isEmpty()) {
                html.append("<tr><td colspan='3' align='center'>No family members recorded.</td></tr>");
            } else {
                for (FamilyDetail detail : familyDetails) {
                    html.append("<tr>");
                    html.append("<td align='center'>").append(escapeHtml(detail.getMemberName())).append("</td>");
                    html.append("<td align='center'>").append(escapeHtml(detail.getRelationship())).append("</td>");
                    html.append("<td align='center'>")
                            .append(detail.getDob() != null ? detail.getDob() : "-")
                            .append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");

            html.append("<br><br>");

            /* =========================
             * Leave History
             * ========================= */
            html.append("<h2 align='center'>Leave History (").append(year).append(")</h2>");
            html.append("<table border='1' cellpadding='8' cellspacing='0' width='100%'>");
            html.append("<tr>");
            html.append("<th align='center'>Leave ID</th>");
            html.append("<th align='center'>Type</th>");
            html.append("<th align='center'>Start Date</th>");
            html.append("<th align='center'>End Date</th>");
            html.append("<th align='center'>Status</th>");
            html.append("<th align='center'>Reason</th>");
            html.append("<th align='center'>Applied On</th>");
            html.append("</tr>");

            if (leaves.isEmpty()) {
                html.append("<tr><td colspan='7' align='center'>No leave applications for this year.</td></tr>");
            } else {
                for (LeaveApplication leave : leaves) {
                    html.append("<tr>");
                    html.append("<td align='center'>").append(leave.getLeaveId()).append("</td>");
                    html.append("<td align='center'>").append(escapeHtml(leave.getLeaveType())).append("</td>");
                    html.append("<td align='center'>").append(leave.getStartDate()).append("</td>");
                    html.append("<td align='center'>").append(leave.getEndDate()).append("</td>");
                    html.append("<td align='center'>").append(escapeHtml(leave.getStatus())).append("</td>");
                    html.append("<td align='center'>").append(escapeHtml(leave.getReason())).append("</td>");
                    html.append("<td align='center'>")
                            .append(leave.getAppliedOn() != null ? leave.getAppliedOn() : "-")
                            .append("</td>");
                    html.append("</tr>");
                }
            }
            html.append("</table>");

            html.append("</td></tr></table>");
            html.append("</body>");
            html.append("</html>");
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
    public boolean changePassword(int empId, String currentPassword, String newPassword)
            throws RemoteException {
        try {
            if (newPassword == null || newPassword.isBlank()) {
                throw new RemoteException("New password cannot be empty.");
            }

            String currentHash = db.getPasswordHashByEmpId(empId);
            if (currentHash == null || !BCrypt.checkpw(currentPassword, currentHash)) {
                throw new RemoteException("Current password is incorrect.");
            }

            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            return db.updatePasswordHash(empId, newHash);
        } catch (RemoteException e) {
            throw e;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to change password for empId: " + empId, e);
            throw new RemoteException("Failed to change password: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error changing password", e);
            throw new RemoteException("Failed to change password: " + e.getMessage(), e);
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
    public boolean removeFamilyDetail(int empId, int detailId) throws RemoteException {
        try {
            return db.deleteFamilyDetail(empId, detailId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to remove family detail " + detailId + " for empId: " + empId, e);
            throw new RemoteException("Failed to remove family detail: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error removing family detail", e);
            throw new RemoteException("Failed to remove family detail: " + e.getMessage(), e);
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
    public synchronized boolean updateLeaveStatus(int leaveId, String status, int approvedBy)
            throws RemoteException {
        try {
            if (approvedBy <= 0) {
                throw new RemoteException("Approved-by employee ID is required.");
            }

            String normalizedStatus = status == null ? "" : status.toUpperCase();
            if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
                throw new RemoteException("Leave status must be APPROVED or REJECTED.");
            }

            boolean updated = db.updatePendingLeaveStatus(leaveId, normalizedStatus, approvedBy);
            if (!updated) {
                throw new RemoteException("Pending leave application not found for ID: " + leaveId);
            }
            return true;
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
