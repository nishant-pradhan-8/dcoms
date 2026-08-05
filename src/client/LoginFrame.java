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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
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

        // Apply native system Look and Feel to avoid the blocky default appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback gracefully if system theme fails to load
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(360, 260); // Sized more compactly to match layout scale
        setLocationRelativeTo(null);

        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");

        // Main structural wrapper
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(24, 24, 24, 24)); // Soft outer frame margin
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0; // Expand fully across horizontally

        // --- Username Label ---
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0); // 4px space beneath label
        panel.add(new JLabel("Username:"), gbc);

        // --- Username Field ---
        gbc.gridy = 1;
        gbc.ipady = 8; // Adds subtle internal height to modernise field proportions
        gbc.insets = new Insets(0, 0, 14, 0); // 14px space before the next section
        panel.add(usernameField, gbc);

        // --- Password Label ---
        gbc.gridy = 2;
        gbc.ipady = 0; // Reset internal padding for structural label text
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Password:"), gbc);

        // --- Password Field ---
        gbc.gridy = 3;
        gbc.ipady = 8; // Match internal height of the username field
        gbc.insets = new Insets(0, 0, 20, 0); // Generous 20px buffer before final action button
        panel.add(passwordField, gbc);

        // --- Login Button ---
        loginButton.addActionListener(e -> performLogin());
        gbc.gridy = 4;
        gbc.ipady = 6; // Balance button height against input heights
        gbc.insets = new Insets(0, 0, 0, 0);
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