package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;

public class ReportPanel extends JPanel {

    private final HRMService service;
    private final JTextField empIdField;
    private final JTextField yearField;
    private final JEditorPane reportPane;
    private final JButton generateButton;

    public ReportPanel(HRMService service, Employee loggedInEmployee) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        empIdField = new JTextField(10);
        yearField = new JTextField(10);
        generateButton = new JButton("Generate");

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        formPanel.add(new JLabel("Employee ID:"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.ipady = 4;
        formPanel.add(empIdField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.ipady = 0;
        formPanel.add(new JLabel("Year:"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.ipady = 4;
        formPanel.add(yearField, gbc);

        generateButton.addActionListener(e -> generateReport());
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.ipady = 4;
        formPanel.add(generateButton, gbc);

        yearField.setText(String.valueOf(java.time.Year.now().getValue()));

        reportPane = new JEditorPane();
        reportPane.setEditable(false);
        reportPane.setContentType("text/html");
        reportPane.setText("<html><body><p'>Generate a report to view it here.</p></body></html>");

        add(formPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportPane), BorderLayout.CENTER);
    }

    private void generateReport() {
        String empIdText = empIdField.getText().trim();
        String yearText = yearField.getText().trim();

        if (empIdText.isEmpty() || yearText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter employee ID and year.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int empId;
        int year;
        try {
            empId = Integer.parseInt(empIdText);
            year = Integer.parseInt(yearText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Employee ID and year must be valid numbers.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        generateButton.setEnabled(false);
        reportPane.setContentType("text/html");
        reportPane.setText("<html><body><p>Generating report...</p></body></html>");

        SwingWorker<byte[], Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected byte[] doInBackground() {
                try {
                    return service.generateYearlyReport(empId, year);
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                generateButton.setEnabled(true);
                if (error != null) {
                    reportPane.setText("<html><body></body></html>");
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            error.getMessage(), "Report Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    byte[] report = get();
                    if (report == null) {
                        reportPane.setText("<html><body></body></html>");
                        JOptionPane.showMessageDialog(ReportPanel.this,
                                "No report data returned.",
                                "Report Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String html = new String(report, StandardCharsets.UTF_8);
                    reportPane.setContentType("text/html");
                    reportPane.setText(html);
                    reportPane.setCaretPosition(0);
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            "Report generated successfully.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    reportPane.setText("<html><body></body></html>");
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
