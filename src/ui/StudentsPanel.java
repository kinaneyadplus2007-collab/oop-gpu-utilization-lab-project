package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import logic.AccountService;
import logic.Validator;
import model.RuleException;
import model.Student;

public class StudentsPanel extends JPanel implements Refreshable {
    private AccountService accounts;
    private final Runnable onChange;
    private Supplier<LocalDateTime> clock;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JTextField minutesField;
    private JTextField reasonField;
    private List<Student> shown;

    public StudentsPanel(AccountService accounts, Supplier<LocalDateTime> clock) {
        this(accounts, clock, () -> { });
    }

    public StudentsPanel(AccountService accounts, Supplier<LocalDateTime> clock,
                         Runnable onChange) {
        this.accounts = accounts;
        this.clock = clock;
        this.onChange = onChange == null ? () -> { } : onChange;
        setLayout(new BorderLayout());

        JPanel topRow = new JPanel(new FlowLayout());
        topRow.add(new JLabel("University ID:"));

        searchField = new JTextField(10);
        topRow.add(searchField);

        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show all");

        topRow.add(searchButton);
        topRow.add(showAllButton);
        add(topRow, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{
                        "University ID", "Name", "Email", "Phone",
                        "Allocated", "Used", "Reserved", "Available"
                }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new FlowLayout());
        bottomRow.add(new JLabel("Extra minutes:"));

        minutesField = new JTextField(5);
        bottomRow.add(minutesField);

        bottomRow.add(new JLabel("Reason:"));
        reasonField = new JTextField(20);
        bottomRow.add(reasonField);

        JButton grantButton = new JButton("Grant extra time");
        bottomRow.add(grantButton);
        add(bottomRow, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> search());
        showAllButton.addActionListener(e -> refresh());
        grantButton.addActionListener(e -> grant());

        refresh();
    }

    private void show(List<Student> students) {
        tableModel.setRowCount(0);
        shown = students;

        for (Student s : shown) {
            tableModel.addRow(new Object[]{
                    s.getUsername(),
                    s.getName(),
                    s.getEmail(),
                    s.getPhone(),
                    s.getAllowance().getAllocated(),
                    s.getAllowance().getUsed(),
                    s.getAllowance().getReserved(),
                    s.getAllowance().available()
            });
        }
    }

    @Override
    public void refresh() {
        show(accounts.getStudents());
    }

    private void search() {
        String id = searchField.getText().trim();

        if (id.isEmpty()) {
            refresh();
            return;
        }

        Student s = accounts.findStudent(id);
        if (s == null) {
            JOptionPane.showMessageDialog(this, "No student with ID " + id);
            return;
        }

        List<Student> one = new ArrayList<>();
        one.add(s);
        show(one);
    }

    private void grant() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a student first.");
            return;
        }

        if (!Validator.positiveInt(minutesField.getText())) {
            JOptionPane.showMessageDialog(
                    this, "Enter a positive whole number of minutes.");
            return;
        }

        try {
            accounts.grantTime(
                    shown.get(row),
                    Integer.parseInt(minutesField.getText().trim()),
                    reasonField.getText(),
                    accounts.getAdmin().getName(),
                    clock.get()
            );

            minutesField.setText("");
            reasonField.setText("");
            refresh();
            onChange.run();
        } catch (RuleException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
}
