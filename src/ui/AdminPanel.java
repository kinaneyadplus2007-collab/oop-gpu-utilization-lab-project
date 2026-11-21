package ui;

import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import javax.swing.*;
import logic.AccountService;

public class AdminPanel extends JPanel {
    private RequestsPanel requests;
    private StudentsPanel students;

    public AdminPanel(AccountService accounts, Supplier<LocalDateTime> clock) {
        setLayout(new BorderLayout());

        requests = new RequestsPanel(accounts);
        students = new StudentsPanel(accounts, clock);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Requests", requests);
        tabs.addTab("Students", students);

        tabs.addChangeListener(e -> {
            requests.refresh();
            students.refresh();
        });

        add(tabs, BorderLayout.CENTER);
    }
}