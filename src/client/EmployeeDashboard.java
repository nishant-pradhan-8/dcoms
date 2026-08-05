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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.List;

public class EmployeeDashboard extends JFrame {

    private final HRMService service;
    private final Employee loggedInEmployee;

    private JLabel annualLeaveLabel;
    private JLabel sickLeaveLabel;
    private DefaultTableModel leaveHistoryTableModel;

    public EmployeeDashboard(HRMService service, Employee loggedInEmployee) {
        super("HRM System - Employee Dashboard");
        this.service = service;
        this.loggedInEmployee = loggedInEmployee;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("My Profile", new ProfilePanel(service, loggedInEmployee));
        tabbedPane.addTab("Family Details", new FamilyDetailsPanel(service, loggedInEmployee));
        tabbedPane.addTab("Leave Balance", createLeaveBalancePanel());
        tabbedPane.addTab("Apply Leave", new LeaveManagementPanel(service, loggedInEmployee));
        tabbedPane.addTab("Leave History", createLeaveHistoryPanel());

        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index == 2) {
                loadLeaveBalance();
            } else if (index == 4) {
                loadLeaveHistory();
            }
        });

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel header = new JLabel("Logged in as: " + loggedInEmployee.getFirstName()
                + " " + loggedInEmployee.getLastName() + " (Employee)");
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        headerPanel.add(header, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
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

    private JPanel createLeaveBalancePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        annualLeaveLabel = new JLabel("Annual Leave: --");
        sickLeaveLabel = new JLabel("Sick Leave: --");

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);
        contentPanel.add(annualLeaveLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        contentPanel.add(sickLeaveLabel, gbc);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadLeaveBalance());
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(refreshButton, gbc);

        panel.add(contentPanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createLeaveHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        leaveHistoryTableModel = new DefaultTableModel(
                new String[]{"Leave ID", "Type", "Start", "End", "Status", "Reason", "Applied On"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(leaveHistoryTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadLeaveHistory());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadLeaveBalance() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected int[] doInBackground() {
                try {
                    int annual = service.getLeaveBalance(loggedInEmployee.getEmpId(), "ANNUAL");
                    int sick = service.getLeaveBalance(loggedInEmployee.getEmpId(), "SICK");
                    return new int[]{annual, sick};
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(EmployeeDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    int[] balances = get();
                    if (balances != null) {
                        annualLeaveLabel.setText("Annual Leave: " + balances[0] + " day(s)");
                        sickLeaveLabel.setText("Sick Leave: " + balances[1] + " day(s)");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EmployeeDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadLeaveHistory() {
        SwingWorker<List<LeaveApplication>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<LeaveApplication> doInBackground() {
                try {
                    return service.getLeaveHistory(loggedInEmployee.getEmpId());
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(EmployeeDashboard.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    leaveHistoryTableModel.setRowCount(0);
                    List<LeaveApplication> history = get();
                    if (history == null) {
                        return;
                    }
                    for (LeaveApplication leave : history) {
                        leaveHistoryTableModel.addRow(new Object[]{
                                leave.getLeaveId(),
                                leave.getLeaveType(),
                                leave.getStartDate(),
                                leave.getEndDate(),
                                leave.getStatus(),
                                leave.getReason(),
                                leave.getAppliedOn()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EmployeeDashboard.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
