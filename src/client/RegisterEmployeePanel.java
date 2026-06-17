package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class RegisterEmployeePanel extends JPanel {

    private final HRMService service;
    private final JTextField firstNameField;
    private final JTextField lastNameField;
    private final JTextField icPassportField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton registerButton;

    public RegisterEmployeePanel(HRMService service, Employee loggedInEmployee) {
        this.service = service;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        firstNameField = new JTextField(20);
        lastNameField = new JTextField(20);
        icPassportField = new JTextField(20);
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        registerButton = new JButton("Register");

        addField(gbc, 0, "First Name:", firstNameField);
        addField(gbc, 1, "Last Name:", lastNameField);
        addField(gbc, 2, "IC/Passport:", icPassportField);
        addField(gbc, 3, "Username:", usernameField);
        addField(gbc, 4, "Password:", passwordField);

        registerButton.addActionListener(e -> registerEmployee());
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        add(registerButton, gbc);
    }

    private void addField(GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        add(field, gbc);
    }

    private void registerEmployee() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String icPassport = icPassportField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (firstName.isEmpty() || lastName.isEmpty() || icPassport.isEmpty()
                || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in all fields.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        registerButton.setEnabled(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected String doInBackground() {
                try {
                    return service.registerEmployee(firstName, lastName, icPassport, username, password);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
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
                    JOptionPane.showMessageDialog(RegisterEmployeePanel.this,
                            get(), "Success", JOptionPane.INFORMATION_MESSAGE);
                    firstNameField.setText("");
                    lastNameField.setText("");
                    icPassportField.setText("");
                    usernameField.setText("");
                    passwordField.setText("");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RegisterEmployeePanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
