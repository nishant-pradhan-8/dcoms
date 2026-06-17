package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;

public class ProfilePanel extends JPanel {

    private final HRMService service;
    private final Employee employee;
    private final JTextField firstNameField;
    private final JTextField lastNameField;
    private final JTextField icPassportField;
    private final JButton saveButton;

    public ProfilePanel(HRMService service, Employee employee) {
        this.service = service;
        this.employee = employee;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        firstNameField = new JTextField(employee.getFirstName(), 20);
        lastNameField = new JTextField(employee.getLastName(), 20);
        icPassportField = new JTextField(employee.getIcPassport(), 20);
        saveButton = new JButton("Save");

        addRow(gbc, 0, "First Name:", firstNameField);
        addRow(gbc, 1, "Last Name:", lastNameField);
        addRow(gbc, 2, "IC/Passport:", icPassportField);

        saveButton.addActionListener(e -> saveProfile());
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(saveButton, gbc);
    }

    private void addRow(GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        add(field, gbc);
    }

    private void saveProfile() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String icPassport = icPassportField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || icPassport.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Employee updated = new Employee(
                employee.getEmpId(),
                firstName,
                lastName,
                icPassport,
                employee.getUsername(),
                employee.getRole()
        );

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
                        employee.setFirstName(firstName);
                        employee.setLastName(lastName);
                        employee.setIcPassport(icPassport);
                        JOptionPane.showMessageDialog(ProfilePanel.this,
                                "Profile updated successfully.",
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
}
