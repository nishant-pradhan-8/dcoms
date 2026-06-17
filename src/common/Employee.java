package common;

import java.io.Serializable;

public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private int empId;
    private String firstName;
    private String lastName;
    private String icPassport;
    private String username;
    private String role;

    public Employee() {
    }

    public Employee(int empId, String firstName, String lastName,
                    String icPassport, String username, String role) {
        this.empId = empId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.icPassport = icPassport;
        this.username = username;
        this.role = role;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getIcPassport() {
        return icPassport;
    }

    public void setIcPassport(String icPassport) {
        this.icPassport = icPassport;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
