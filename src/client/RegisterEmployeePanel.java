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
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.SecureRandom;

public class RegisterEmployeePanel extends JPanel {

    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int PASSWORD_LENGTH = 8;
    private static final int MAX_USERNAME_ATTEMPTS = 20;

    private final HRMService service;
    private final JTextField firstNameField;
    private final JTextField lastNameField;
    private final JTextField icPassportField;
    private final JButton registerButton;
    private final SecureRandom random = new SecureRandom();

    public RegisterEmployeePanel(HRMService service, Employee loggedInEmployee) {
        this.service = service;

        setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        firstNameField = new JTextField(20);
        lastNameField = new JTextField(20);
        icPassportField = new JTextField(20);
        registerButton = new JButton("Register");

        int row = 0;
        row = addField(formPanel, gbc, row, "First Name:", firstNameField);
        row = addField(formPanel, gbc, row, "Last Name:", lastNameField);
        row = addField(formPanel, gbc, row, "IC/Passport:", icPassportField);

        registerButton.addActionListener(e -> registerEmployee());
        gbc.gridy = row;
        gbc.insets = new Insets(8, 0, 0, 0);
        gbc.ipady = 4;
        formPanel.add(registerButton, gbc);

        add(formPanel, BorderLayout.NORTH);
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
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

    private void registerEmployee() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String icPassport = icPassportField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || icPassport.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in First Name, Last Name, and IC/Passport.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String baseUsername = generateUsername(firstName, lastName);
        if (baseUsername.isEmpty() || baseUsername.equals(".")) {
            JOptionPane.showMessageDialog(this,
                    "First Name and Last Name must contain letters to generate a username.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        registerButton.setEnabled(false);
        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected String[] doInBackground() {
                String password = generatePassword();
                Exception lastError = null;

                for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
                    String username = attempt == 0 ? baseUsername : baseUsername + (attempt + 1);
                    try {
                        String serverMessage = service.registerEmployee(
                                firstName, lastName, icPassport, username, password);
                        return new String[]{username, password, serverMessage};
                    } catch (Exception e) {
                        lastError = e;
                        if (!isDuplicateUsernameError(e)) {
                            error = e;
                            return null;
                        }
                    }
                }

                error = lastError != null
                        ? lastError
                        : new Exception("Could not generate a unique username. Please try again.");
                return null;
            }

            @Override
            protected void done() {
                registerButton.setEnabled(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(RegisterEmployeePanel.this,
                            error.getMessage(), "Registration Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    String[] result = get();
                    if (result == null) {
                        JOptionPane.showMessageDialog(RegisterEmployeePanel.this,
                                "Registration failed.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    showCopyableCredentials(result[2], result[0], result[1]);
                    firstNameField.setText("");
                    lastNameField.setText("");
                    icPassportField.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RegisterEmployeePanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String generateUsername(String firstName, String lastName) {
        String first = sanitizeNamePart(firstName);
        String last = sanitizeNamePart(lastName);
        if (first.isEmpty() && last.isEmpty()) {
            return "";
        }
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + "." + last;
    }

    private String sanitizeNamePart(String value) {
        return value.toLowerCase().replaceAll("[^a-z]", "");
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private boolean isDuplicateUsernameError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("duplicate") || lower.contains("unique");
    }

    private void showCopyableCredentials(String serverMessage, String username, String password) {
        String message = serverMessage
                + "\n\nGenerated credentials (copy & share with the employee):"
                + "\nUsername: " + username
                + "\nTemporary password: " + password;

        JTextArea textArea = new JTextArea(message);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretPosition(0);
        textArea.selectAll();

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(420, 150));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
