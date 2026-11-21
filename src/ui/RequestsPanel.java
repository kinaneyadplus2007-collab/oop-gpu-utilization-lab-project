package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import logic.AccountService;
import logic.Validator;
import model.AccessRequest;
import model.RuleException;

public class RequestsPanel extends JPanel implements Refreshable {
    private AccountService accounts;
    private final Runnable onChange;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField minutesField;
    private List<AccessRequest> shown;

    public RequestsPanel(AccountService accounts) {
        this(accounts, () -> { });
    }

    public RequestsPanel(AccountService accounts, Runnable onChange) {
        this.accounts = accounts;
        this.onChange = onChange == null ? () -> { } : onChange;
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new String[]{"University ID", "Name", "Email", "Phone", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout());
        buttonRow.add(new JLabel("Initial minutes:"));

        minutesField = new JTextField(
                String.valueOf(AccountService.DEFAULT_MINUTES), 5);
        buttonRow.add(minutesField);

        JButton approveButton = new JButton("Approve");
        JButton rejectButton = new JButton("Reject");
        JButton refreshButton = new JButton("Refresh");

        approveButton.addActionListener(e -> approve());
        rejectButton.addActionListener(e -> reject());
        refreshButton.addActionListener(e -> refresh());

        buttonRow.add(approveButton);
        buttonRow.add(rejectButton);
        buttonRow.add(refreshButton);

        add(buttonRow, BorderLayout.SOUTH);

        refresh();
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        shown = accounts.getRequests();

        for (AccessRequest r : shown) {
            tableModel.addRow(new Object[]{
                    r.getUniversityId(),
                    r.getName(),
                    r.getEmail(),
                    r.getPhone(),
                    r.getStatus()
            });
        }
    }

    private AccessRequest selected() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a request first.");
            return null;
        }

        return shown.get(row);
    }

    private void approve() {
        AccessRequest request = selected();
        if (request == null) {
            return;
        }

        if (!Validator.positiveInt(minutesField.getText())) {
            JOptionPane.showMessageDialog(
                    this, "Enter a positive whole number of minutes.");
            return;
        }

        try {
            accounts.approve(
                    request, Integer.parseInt(minutesField.getText().trim()));
            refresh();
            onChange.run();
        } catch (RuleException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void reject() {
        AccessRequest request = selected();
        if (request == null) {
            return;
        }

        try {
            accounts.reject(request);
            refresh();
            onChange.run();
        } catch (RuleException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
}
