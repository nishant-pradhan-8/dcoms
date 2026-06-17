package client;

import common.Employee;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;

public class LoginFrame extends JFrame {

    private final HRMService service;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;

    public LoginFrame(HRMService service) {
        super("HRM System - Login");
        this.service = service;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(passwordField, gbc);

        loginButton.addActionListener(e -> performLogin());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(loginButton, gbc);

        add(panel);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter both username and password.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        loginButton.setEnabled(false);

        SwingWorker<Employee, Void> worker = new SwingWorker<>() {
            private RemoteException loginError;

            @Override
            protected Employee doInBackground() {
                try {
                    return service.login(username, password);
                } catch (RemoteException e) {
                    loginError = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    if (loginError != null) {
                        JOptionPane.showMessageDialog(
                                LoginFrame.this,
                                loginError.getMessage(),
                                "Login Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    Employee employee = get();
                    if (employee == null) {
                        JOptionPane.showMessageDialog(
                                LoginFrame.this,
                                "Invalid credentials.",
                                "Login Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    dispose();
                    if ("HR".equalsIgnoreCase(employee.getRole())) {
                        new HRDashboard(service, employee).setVisible(true);
                    } else {
                        new EmployeeDashboard(service, employee).setVisible(true);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            LoginFrame.this,
                            "Login failed: " + e.getMessage(),
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }
}
