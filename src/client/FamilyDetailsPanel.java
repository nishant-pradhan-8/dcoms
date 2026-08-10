package client;

import common.Employee;
import common.FamilyDetail;
import common.HRMService;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.sql.Date;
import java.util.List;

public class FamilyDetailsPanel extends JPanel {

    private final HRMService service;
    private final Employee employee;
    private final DefaultTableModel tableModel;

    public FamilyDetailsPanel(HRMService service, Employee employee) {
        this.service = service;
        this.employee = employee;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Member Name", "Relationship", "Date of Birth"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        JButton removeButton = new JButton("Remove");
        addButton.addActionListener(e -> showAddDialog());
        removeButton.addActionListener(e -> removeSelectedFamilyMember(table));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadFamilyDetails();
    }

    private void removeSelectedFamilyMember(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a family member to remove.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int detailId = (int) tableModel.getValueAt(selectedRow, 0);
        String memberName = String.valueOf(tableModel.getValueAt(selectedRow, 1));

        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove family member \"" + memberName + "\"?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected Boolean doInBackground() {
                try {
                    return service.removeFamilyDetail(employee.getEmpId(), detailId);
                } catch (RemoteException e) {
                    error = e;
                    return false;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(FamilyDetailsPanel.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    if (Boolean.TRUE.equals(get())) {
                        loadFamilyDetails();
                    } else {
                        JOptionPane.showMessageDialog(FamilyDetailsPanel.this,
                                "Failed to remove family member.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FamilyDetailsPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                "Add Family Member",
                true
        );
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nameField = new JTextField(20);
        JTextField relationshipField = new JTextField(20);
        JTextField dobField = new JTextField(20);

        int row = 0;
        row = addDialogField(formPanel, gbc, row, "Member Name:", nameField);
        row = addDialogField(formPanel, gbc, row, "Relationship:", relationshipField);
        addDialogField(formPanel, gbc, row, "DOB (YYYY-MM-DD):", dobField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submitButton = new JButton("Submit");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        cancelButton.addActionListener(e -> dialog.dispose());

        submitButton.addActionListener(e -> {
            String memberName = nameField.getText().trim();
            String relationship = relationshipField.getText().trim();
            String dobText = dobField.getText().trim();

            if (memberName.isEmpty() || relationship.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Member name and relationship are required.",
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Date dob = null;
            if (!dobText.isEmpty()) {
                try {
                    dob = Date.valueOf(dobText);
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(dialog,
                            "Date of birth must be in YYYY-MM-DD format.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            FamilyDetail detail = new FamilyDetail();
            detail.setMemberName(memberName);
            detail.setRelationship(relationship);
            detail.setDob(dob);

            submitButton.setEnabled(false);
            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                private Exception error;

                @Override
                protected Boolean doInBackground() {
                    try {
                        return service.addFamilyDetail(employee.getEmpId(), detail);
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
                                error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            dialog.dispose();
                            loadFamilyDetails();
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Failed to add family member.",
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

    private int addDialogField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
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

    private void loadFamilyDetails() {
        SwingWorker<List<FamilyDetail>, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected List<FamilyDetail> doInBackground() {
                try {
                    return service.getFamilyDetails(employee.getEmpId());
                } catch (RemoteException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(FamilyDetailsPanel.this,
                            error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    tableModel.setRowCount(0);
                    List<FamilyDetail> details = get();
                    if (details == null) {
                        return;
                    }
                    for (FamilyDetail detail : details) {
                        tableModel.addRow(new Object[]{
                                detail.getDetailId(),
                                detail.getMemberName(),
                                detail.getRelationship(),
                                detail.getDob()
                        });
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(FamilyDetailsPanel.this,
                            e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
