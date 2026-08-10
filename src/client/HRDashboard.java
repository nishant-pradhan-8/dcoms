package client;

import common.Employee;
import common.HRMService;
import common.LeaveApplication;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.List;

public class HRDashboard extends JFrame {

    private final HRMService service;
    private final Employee loggedInEmployee;

    private DefaultTableModel employeesTableModel;
    private JTable employeesTable;
    private DefaultTableModel pendingLeavesTableModel;
    private JTable pendingLeavesTable;
    private List<LeaveApplication> pendingLeaves;

    public HRDashboard(HRMService service, Employee loggedInEmployee) {
        super("HRM System - HR Dashboard");
        this.service = service;
        this.loggedInEmployee = loggedInEmployee;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Register Employee", new RegisterEmployeePanel(service, loggedInEmployee));
        tabbedPane.addTab("View All Employees", createEmployeesPanel());
        tabbedPane.addTab("Approve Leave", createApproveLeavePanel());
        tabbedPane.addTab("Generate Report", new ReportPanel(service, loggedInEmployee));

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 1) {
                loadEmployees();
            } else if (tabbedPane.getSelectedIndex() == 2) {
                loadPendingLeaves();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel header = new JLabel("Logged in as: " + loggedInEmployee.getFirstName()
                + " " + loggedInEmployee.getLastName() + " (HR)");
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        headerPanel.add(header, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        dispose();
        new LoginFrame(service).setVisible(true);
    }

    private JPanel createEmployeesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        employeesTableModel = new DefaultTableModel(
                new String[]{"ID", "First Name", "Last Name", "IC/Passport", "Username", "Role"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        employeesTable = new JTable(employeesTableModel);
        panel.add(new JScrollPane(employeesTable), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        JButton leaveBalanceButton = new JButton("Leave Balance");
        JButton editButton = new JButton("Edit Identity");
        refreshButton.addActionListener(e -> loadEmployees());
        leaveBalanceButton.addActionListener(e -> showLeaveBalanceDialog());
        editButton.addActionListener(e -> showEditIdentityDialog());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(leaveBalanceButton);
        buttonPanel.add(editButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void showLeaveBalanceDialog() {
        int selectedRow = employeesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an employee to view leave balance.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int empId = (int) employeesTableModel.getValueAt(selectedRow, 0);
        String firstName = String.valueOf(employeesTableModel.getValueAt(selectedRow, 1));
        String lastName = String.valueOf(employeesTableModel.getValueAt(selectedRow, 2));

        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected int[] doInBackground() {
                try {
                    int annual = service.getLeaveBalance(empId, "ANNUAL");
                    int sick = service.getLeaveBalance(empId, "SICK");
                    return new int[]{annual, sick};
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    int[] balances = get();
                    if (balances == null) {
                        return;
                    }
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            "Employee: " + firstName + " " + lastName + " (ID: " + empId + ")\n\n"
                                    + "Annual Leave Remaining: " + balances[0] + "\n"
                                    + "Sick Leave Remaining: " + balances[1],
                            "Leave Balance", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showEditIdentityDialog() {
        int selectedRow = employeesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an employee to edit.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int empId = (int) employeesTableModel.getValueAt(selectedRow, 0);
        String currentFirstName = String.valueOf(employeesTableModel.getValueAt(selectedRow, 1));
        String currentLastName = String.valueOf(employeesTableModel.getValueAt(selectedRow, 2));
        String currentIcPassport = String.valueOf(employeesTableModel.getValueAt(selectedRow, 3));

        JDialog dialog = new JDialog(this, "Edit Employee Identity", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField firstNameField = new JTextField(currentFirstName, 20);
        JTextField lastNameField = new JTextField(currentLastName, 20);
        JTextField icPassportField = new JTextField(currentIcPassport, 20);

        int row = 0;
        row = addDialogField(formPanel, gbc, row, "First Name:", firstNameField);
        row = addDialogField(formPanel, gbc, row, "Last Name:", lastNameField);
        addDialogField(formPanel, gbc, row, "IC/Passport:", icPassportField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        saveButton.addActionListener(e -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String icPassport = icPassportField.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || icPassport.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "First Name, Last Name, and IC/Passport are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            saveButton.setEnabled(false);
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                private Exception error;

                @Override
                protected Boolean doInBackground() {
                    try {
                        return service.updateEmployeeIdentity(empId, firstName, lastName, icPassport);
                    } catch (RemoteException ex) {
                        error = ex;
                        return false;
                    }
                }

                @Override
                protected void done() {
                    saveButton.setEnabled(true);
                    if (error != null) {
                        JOptionPane.showMessageDialog(dialog,
                                error.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(HRDashboard.this,
                                    "Employee identity updated successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            loadEmployees();
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Failed to update employee identity.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog,
                                ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private int addDialogField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.ipady = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.ipady = 4;
        panel.add(field, gbc);

        return row + 2;
    }

    private JPanel createApproveLeavePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        pendingLeavesTableModel = new DefaultTableModel(
                new String[]{"Leave ID", "Emp ID", "Type", "Start", "End", "Reason", "Applied On"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        pendingLeavesTable = new JTable(pendingLeavesTableModel);
        panel.add(new JScrollPane(pendingLeavesTable), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");

        refreshButton.addActionListener(e -> loadPendingLeaves());
        approveButton.addActionListener(e -> updateSelectedLeaveStatus("APPROVED"));
        rejectButton.addActionListener(e -> updateSelectedLeaveStatus("REJECTED"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadEmployees() {
        SwingWorker<List<Employee>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<Employee> doInBackground() {
                try {
                    return service.getAllEmployees();
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    employeesTableModel.setRowCount(0);
                    List<Employee> employees = get();
                    if (employees == null) {
                        return;
                    }
                    for (Employee emp : employees) {
                        employeesTableModel.addRow(new Object[]{
                                emp.getEmpId(),
                                emp.getFirstName(),
                                emp.getLastName(),
                                emp.getIcPassport(),
                                emp.getUsername(),
                                emp.getRole()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadPendingLeaves() {
        SwingWorker<List<LeaveApplication>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<LeaveApplication> doInBackground() {
                try {
                    return service.getPendingLeaves();
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    pendingLeavesTableModel.setRowCount(0);
                    pendingLeaves = get();
                    if (pendingLeaves == null) {
                        return;
                    }
                    for (LeaveApplication leave : pendingLeaves) {
                        pendingLeavesTableModel.addRow(new Object[]{
                                leave.getLeaveId(),
                                leave.getEmpId(),
                                leave.getLeaveType(),
                                leave.getStartDate(),
                                leave.getEndDate(),
                                leave.getReason(),
                                leave.getAppliedOn()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateSelectedLeaveStatus(String status) {
        int selectedRow = pendingLeavesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a leave application.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int leaveId = (int) pendingLeavesTableModel.getValueAt(selectedRow, 0);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected Boolean doInBackground() {
                try {
                    return service.updateLeaveStatus(leaveId, status, loggedInEmployee.getEmpId());
                } catch (RemoteException e) {
                    error = e;
                    return false;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (Boolean.TRUE.equals(get())) {
                        JOptionPane.showMessageDialog(HRDashboard.this,
                                "Leave application " + status.toLowerCase() + " successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadPendingLeaves();
                    } else {
                        JOptionPane.showMessageDialog(HRDashboard.this,
                                "Failed to update leave status.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(HRDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
