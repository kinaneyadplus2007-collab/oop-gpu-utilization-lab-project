package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import logic.AccountService;
import logic.Validator;
import model.AccessRequest;
import model.RequestStatus;
import model.RuleException;

public class RequestsPanel extends JPanel implements Refreshable {
    private AccountService accounts;
    private final Runnable onChange;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField minutesField;
    private List<AccessRequest> shown = new ArrayList<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"PENDING", "APPROVED", "REJECTED", "All statuses"}
    );
    private final JButton approveButton = new JButton("Approve");
    private final JButton rejectButton = new JButton("Reject");
    private final JButton detailsButton = new JButton("View request");

    public RequestsPanel(AccountService accounts) {
        this(accounts, () -> { });
    }

    public RequestsPanel(AccountService accounts, Runnable onChange) {
        this.accounts = accounts;
        this.onChange = onChange == null ? () -> { } : onChange;
        setLayout(new BorderLayout());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Show requests:"));
        filters.add(statusFilter);
        add(filters, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"University ID", "Name", "Email", "Phone", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout());
        buttonRow.add(new JLabel("Initial minutes:"));

        minutesField = new JTextField(
                String.valueOf(AccountService.DEFAULT_MINUTES), 5);
        buttonRow.add(minutesField);

        JButton refreshButton = new JButton("Refresh");

        approveButton.addActionListener(e -> approve());
        rejectButton.addActionListener(e -> reject());
        refreshButton.addActionListener(e -> refresh());
        detailsButton.addActionListener(e -> showDetails());
        statusFilter.addActionListener(e -> refresh());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        buttonRow.add(approveButton);
        buttonRow.add(rejectButton);
        buttonRow.add(detailsButton);
        buttonRow.add(refreshButton);

        add(buttonRow, BorderLayout.SOUTH);

        refresh();
    }

    @Override
    public void refresh() {
        table.clearSelection();
        tableModel.setRowCount(0);
        shown = new ArrayList<>();
        String filter = (String) statusFilter.getSelectedItem();

        for (AccessRequest r : accounts.getRequests()) {
            if (!"All statuses".equals(filter) && !r.getStatus().name().equals(filter)) {
                continue;
            }

            shown.add(r);
            tableModel.addRow(new Object[]{
                    r.getUniversityId(),
                    r.getName(),
                    r.getEmail(),
                    r.getPhone(),
                    r.getStatus()
            });
        }
        updateButtons();
    }

    private AccessRequest selected() {
        int row = table.getSelectedRow();

        if (row < 0 || row >= table.getRowCount()) {
            return null;
        }

        return shown.get(table.convertRowIndexToModel(row));
    }

    private void updateButtons() {
        AccessRequest request = selected();
        boolean pending = request != null && request.getStatus() == RequestStatus.PENDING;
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(pending);
        minutesField.setEnabled(pending);
        detailsButton.setEnabled(request != null);
    }

    private JPanel requestDetails(AccessRequest request) {
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        String[] labels = {"University ID", "Name", "Email", "Phone", "Status"};
        String[] values = {
            request.getUniversityId(), request.getName(), request.getEmail(),
            request.getPhone(), request.getStatus().toString()
        };

        for (int i = 0; i < labels.length; i++) {
            fields.add(new JLabel(labels[i] + ":"));
            JTextField value = new JTextField(values[i], 24);
            value.setEditable(false);
            fields.add(value);
        }

        JPasswordField password = new JPasswordField(request.getPassword(), 24);
        password.setEditable(false);
        fields.add(new JLabel("Chosen password:"));
        fields.add(password);

        char echo = password.getEchoChar();
        JCheckBox showPassword = new JCheckBox("Show password");
        showPassword.addActionListener(e -> password.setEchoChar(
                showPassword.isSelected() ? (char) 0 : echo
        ));

        JPanel details = new JPanel(new BorderLayout(8, 8));
        details.add(fields, BorderLayout.CENTER);
        details.add(showPassword, BorderLayout.SOUTH);
        return details;
    }

    private void showDetails() {
        AccessRequest request = selected();
        if (request == null) {
            return;
        }

        JOptionPane.showMessageDialog(
                this, requestDetails(request), "Registration request",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void approve() {
        AccessRequest request = selected();
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
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
        if (request == null || request.getStatus() != RequestStatus.PENDING) {
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
