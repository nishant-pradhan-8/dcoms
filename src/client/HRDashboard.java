package client;

import common.Employee;
import common.HRMService;
import common.LeaveApplication;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;
import java.util.List;

public class HRDashboard extends JFrame {

    private final HRMService service;
    private final Employee loggedInEmployee;

    private DefaultTableModel employeesTableModel;
    private DefaultTableModel pendingLeavesTableModel;
    private JTable pendingLeavesTable;
    private List<LeaveApplication> pendingLeaves;

    public HRDashboard(HRMService service, Employee loggedInEmployee) {
        super("HRM System - HR Dashboard");
        this.service = service;
        this.loggedInEmployee = loggedInEmployee;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
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

        JLabel header = new JLabel("Logged in as: " + loggedInEmployee.getFirstName()
                + " " + loggedInEmployee.getLastName() + " (HR)");
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(header, BorderLayout.NORTH);
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

        JTable table = new JTable(employeesTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadEmployees());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
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
                    return service.updateLeaveStatus(leaveId, status);
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
