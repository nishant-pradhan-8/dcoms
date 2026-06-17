package common;

import java.io.Serializable;
import java.sql.Date;

public class FamilyDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private int detailId;
    private int empId;
    private String memberName;
    private String relationship;
    private Date dob;

    public FamilyDetail() {
    }

    public FamilyDetail(int detailId, int empId, String memberName,
                        String relationship, Date dob) {
        this.detailId = detailId;
        this.empId = empId;
        this.memberName = memberName;
        this.relationship = relationship;
        this.dob = dob;
    }

    public int getDetailId() {
        return detailId;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }
}
