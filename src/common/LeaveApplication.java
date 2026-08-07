package common;

import java.io.Serializable;
import java.sql.Date;

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

    public LeaveApplication() {
    }

    public LeaveApplication(int leaveId, int empId, String leaveType, String status,
                            String reason, Date startDate, Date endDate, Date appliedOn,
                            int approvedBy, Date approvedAt) {
        this.leaveId = leaveId;
        this.empId = empId;
        this.leaveType = leaveType;
        this.status = status;
        this.reason = reason;
        this.startDate = startDate;
        this.endDate = endDate;
        this.appliedOn = appliedOn;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    public int getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(int leaveId) {
        this.leaveId = leaveId;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getAppliedOn() {
        return appliedOn;
    }

    public void setAppliedOn(Date appliedOn) {
        this.appliedOn = appliedOn;
    }

    public int getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(int approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }
}
