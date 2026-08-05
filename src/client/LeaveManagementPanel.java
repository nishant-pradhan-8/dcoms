package client;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import common.Employee;
import common.HRMService;
import common.LeaveApplication;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.sql.Date;
import java.time.LocalDate;

public class LeaveManagementPanel extends JPanel {

    private final HRMService service;
    private final Employee employee;
    private final JComboBox<String> leaveTypeCombo;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final JTextArea reasonArea;
    private final JButton submitButton;

    public LeaveManagementPanel(HRMService service, Employee employee) {
        this.service = service;
        this.employee = employee;

        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        leaveTypeCombo = new JComboBox<>(new String[]{"ANNUAL", "SICK", "EMERGENCY"});
        startDatePicker = createDatePicker();
        endDatePicker = createDatePicker();
        reasonArea = new JTextArea(4, 20);
        reasonArea.setLineWrap(true);
        submitButton = new JButton("Submit");

        int row = 0;
        row = addLabelAndComponent(formPanel, gbc, row, "Leave Type:", leaveTypeCombo);
        row = addLabelAndComponent(formPanel, gbc, row, "Start Date:", startDatePicker);
        row = addLabelAndComponent(formPanel, gbc, row, "End Date:", endDatePicker);
        row = addLabelAndTextArea(formPanel, gbc, row, "Reason:", reasonArea);

        submitButton.addActionListener(e -> submitLeave());
        gbc.gridy = row;
        gbc.insets = new Insets(8, 0, 0, 0);
        gbc.ipady = 4;
        formPanel.add(submitButton, gbc);

        add(formPanel, BorderLayout.NORTH);
    }

    private DatePicker createDatePicker() {
        DatePickerSettings settings = new DatePickerSettings();
        settings.setAllowEmptyDates(true);
        settings.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker datePicker = new DatePicker(settings);
        datePicker.getSettings().setDateRangeLimits(LocalDate.now(), null);
        datePicker.setDateToToday();
        return datePicker;
    }

    private int addLabelAndComponent(JPanel panel, GridBagConstraints gbc, int row, String label,
                                     java.awt.Component component) {
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.ipady = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.ipady = 0;
        panel.add(component, gbc);

        return row + 2;
    }

    private int addLabelAndTextArea(JPanel panel, GridBagConstraints gbc, int row, String label,
                                    JTextArea textArea) {
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel(label), gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(new JScrollPane(textArea), gbc);

        return row + 2;
    }

    private void submitLeave() {
        String leaveType = (String) leaveTypeCombo.getSelectedItem();
        LocalDate startLocal = startDatePicker.getDate();
        LocalDate endLocal = endDatePicker.getDate();
        String reason = reasonArea.getText().trim();

        if (startLocal == null || endLocal == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select both start date and end date.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (startLocal.isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this,
                    "Start date cannot be before today's date.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (endLocal.isBefore(startLocal)) {
            JOptionPane.showMessageDialog(this,
                    "End date cannot be before start date.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date startDate = Date.valueOf(startLocal);
        Date endDate = Date.valueOf(endLocal);

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
                    startDatePicker.clear();
                    endDatePicker.clear();
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
