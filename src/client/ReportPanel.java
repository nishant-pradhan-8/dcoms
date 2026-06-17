package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
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
    private final JTextArea reportArea;
    private final JButton generateButton;

    public ReportPanel(HRMService service, Employee loggedInEmployee) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        empIdField = new JTextField(10);
        yearField = new JTextField(10);
        generateButton = new JButton("Generate");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Employee ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(empIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(yearField, gbc);

        generateButton.addActionListener(e -> generateReport());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formPanel.add(generateButton, gbc);

        yearField.setText(String.valueOf(java.time.Year.now().getValue()));

        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);

        add(formPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportArea), BorderLayout.CENTER);
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
        reportArea.setText("Generating report...");

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
                    reportArea.setText("");
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            error.getMessage(), "Report Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    byte[] report = get();
                    if (report == null) {
                        reportArea.setText("");
                        JOptionPane.showMessageDialog(ReportPanel.this,
                                "No report data returned.",
                                "Report Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String reportText = new String(report, StandardCharsets.UTF_8);
                    reportArea.setText(stripHtmlTags(reportText));
                    reportArea.setCaretPosition(0);
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            "Report generated successfully.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    reportArea.setText("");
                    JOptionPane.showMessageDialog(ReportPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String stripHtmlTags(String html) {
        return html
                .replaceAll("(?i)</h1>", "\n\n")
                .replaceAll("(?i)</h2>", "\n\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</tr>", "\n")
                .replaceAll("(?i)</th>", "\t")
                .replaceAll("(?i)</td>", "\t")
                .replaceAll("(?i)<br/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim();
    }
}
