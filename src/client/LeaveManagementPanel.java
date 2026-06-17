package client;

import common.Employee;
import common.HRMService;
import common.LeaveApplication;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.sql.Date;

public class LeaveManagementPanel extends JPanel {

    private final HRMService service;
    private final Employee employee;
    private final JComboBox<String> leaveTypeCombo;
    private final JTextField startDateField;
    private final JTextField endDateField;
    private final JTextArea reasonArea;
    private final JButton submitButton;

    public LeaveManagementPanel(HRMService service, Employee employee) {
        this.service = service;
        this.employee = employee;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        leaveTypeCombo = new JComboBox<>(new String[]{"ANNUAL", "SICK", "EMERGENCY"});
        startDateField = new JTextField(15);
        endDateField = new JTextField(15);
        reasonArea = new JTextArea(4, 20);
        reasonArea.setLineWrap(true);
        submitButton = new JButton("Submit");

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Leave Type:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(leaveTypeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        add(new JLabel("Start Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(startDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        add(new JLabel("End Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(endDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        add(new JLabel("Reason:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        add(reasonArea, gbc);

        submitButton.addActionListener(e -> submitLeave());
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(submitButton, gbc);
    }

    private void submitLeave() {
        String leaveType = (String) leaveTypeCombo.getSelectedItem();
        String startText = startDateField.getText().trim();
        String endText = endDateField.getText().trim();
        String reason = reasonArea.getText().trim();

        if (startText.isEmpty() || endText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Start date and end date are required.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date startDate;
        Date endDate;
        try {
            startDate = Date.valueOf(startText);
            endDate = Date.valueOf(endText);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    "Dates must be in YYYY-MM-DD format.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LeaveApplication application = new LeaveApplication();
        application.setLeaveType(leaveType);
        application.setStartDate(startDate);
        application.setEndDate(endDate);
        application.setReason(reason);

        submitButton.setEnabled(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected String doInBackground() {
                try {
                    return service.applyLeave(employee.getEmpId(), application);
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(LeaveManagementPanel.this,
                            error.getMessage(), "Application Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    JOptionPane.showMessageDialog(LeaveManagementPanel.this,
                            get(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    startDateField.setText("");
                    endDateField.setText("");
                    reasonArea.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(LeaveManagementPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
