package common;

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
