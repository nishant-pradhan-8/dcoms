package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;

public class ProfilePanel extends JPanel {

    private static final Color READ_ONLY_BACKGROUND = new Color(230, 230, 230);
    private static final Color READ_ONLY_FOREGROUND = new Color(90, 90, 90);

    private final HRMService service;
    private final Employee employee;
    private final JTextField firstNameField;
    private final JTextField lastNameField;
    private final JTextField icPassportField;
    private final JTextField phoneField;
    private final JTextField emailField;
    private final JTextField addressField;
    private final JButton changePasswordButton;
    private final JButton saveButton;

    public ProfilePanel(HRMService service, Employee employee) {
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

        firstNameField = new JTextField(nullToEmpty(employee.getFirstName()), 20);
        lastNameField = new JTextField(nullToEmpty(employee.getLastName()), 20);
        icPassportField = new JTextField(nullToEmpty(employee.getIcPassport()), 20);
        styleReadOnlyField(firstNameField);
        styleReadOnlyField(lastNameField);
        styleReadOnlyField(icPassportField);

        phoneField = new JTextField(nullToEmpty(employee.getPhoneNumber()), 20);
        emailField = new JTextField(nullToEmpty(employee.getEmail()), 20);
        addressField = new JTextField(nullToEmpty(employee.getAddress()), 20);
        changePasswordButton = new JButton("Change Password");
        saveButton = new JButton("Save");

        int row = 0;
        row = addField(formPanel, gbc, row, "First Name:", firstNameField);
        row = addField(formPanel, gbc, row, "Last Name:", lastNameField);
        row = addField(formPanel, gbc, row, "IC/Passport:", icPassportField);
        row = addField(formPanel, gbc, row, "Phone:", phoneField);
        row = addField(formPanel, gbc, row, "Email:", emailField);
        row = addField(formPanel, gbc, row, "Address:", addressField);

        changePasswordButton.addActionListener(e -> showChangePasswordDialog());
        JPanel changePasswordPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        changePasswordPanel.setOpaque(false);
        changePasswordPanel.add(changePasswordButton);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 0, 8, 0);
        gbc.ipady = 0;
        gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(changePasswordPanel, gbc);

        saveButton.addActionListener(e -> saveProfile());
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.ipady = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(saveButton, gbc);

        add(formPanel, BorderLayout.NORTH);
    }

    private void styleReadOnlyField(JTextField field) {
        field.setEditable(false);
        field.setBackground(READ_ONLY_BACKGROUND);
        field.setForeground(READ_ONLY_FOREGROUND);
        field.setDisabledTextColor(READ_ONLY_FOREGROUND);
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridy = row;
        gbc.insets = new Insets(0, 0, 4, 0);
        gbc.ipady = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), gbc);

        gbc.gridy = row + 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.ipady = 4;
        panel.add(field, gbc);

        return row + 2;
    }

    private void showChangePasswordDialog() {
        Frame owner = (Frame) javax.swing.SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Change Password", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JPasswordField currentPasswordField = new JPasswordField(20);
        JPasswordField newPasswordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);

        int row = 0;
        row = addPasswordField(formPanel, gbc, row, "Current Password:", currentPasswordField);
        row = addPasswordField(formPanel, gbc, row, "New Password:", newPasswordField);
        addPasswordField(formPanel, gbc, row, "Confirm New Password:", confirmPasswordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Update Password");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());
        submitButton.addActionListener(e -> {
            String currentPassword = new String(currentPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please fill in all password fields.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dialog,
                        "New password and confirmation do not match.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(dialog,
                        "New password must be at least 6 characters.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            submitButton.setEnabled(false);
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                private Exception error;

                @Override
                protected Boolean doInBackground() {
                    try {
                        return service.changePassword(employee.getEmpId(), currentPassword, newPassword);
                    } catch (RemoteException ex) {
                        error = ex;
                        return false;
                    }
                }

                @Override
                protected void done() {
                    submitButton.setEnabled(true);
                    if (error != null) {
                        JOptionPane.showMessageDialog(dialog,
                                error.getMessage(), "Change Password Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(ProfilePanel.this,
                                    "Password changed successfully.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Failed to change password.",
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

    private int addPasswordField(JPanel panel, GridBagConstraints gbc, int row, String label,
                                 JPasswordField field) {
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

    private void saveProfile() {
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        Employee updated = new Employee();
        updated.setPhoneNumber(phone.isEmpty() ? null : phone);
        updated.setEmail(email.isEmpty() ? null : email);
        updated.setAddress(address.isEmpty() ? null : address);

        saveButton.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected Boolean doInBackground() {
                try {
                    return service.updateProfile(employee.getEmpId(), updated);
                } catch (RemoteException e) {
                    error = e;
                    return false;
                }
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ProfilePanel.this,
                            error.getMessage(), "Update Failed", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (Boolean.TRUE.equals(get())) {
                        employee.setPhoneNumber(updated.getPhoneNumber());
                        employee.setEmail(updated.getEmail());
                        employee.setAddress(updated.getAddress());
                        JOptionPane.showMessageDialog(ProfilePanel.this,
                                "Contact details updated successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ProfilePanel.this,
                                "Failed to update profile.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
