package ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import logic.AccountService;
import model.*;

public class AdminPanelDemo {
    public static void main(String[] args) {
        try {
            List<Student> students = new ArrayList<>();
            List<AccessRequest> requests = new ArrayList<>();
            List<TimeGrant> grants = new ArrayList<>();

            for (String line : Files.readAllLines(Path.of("data/students.txt"))) {
                if (!line.isBlank()) {
                    students.add(Student.fromLine(line));
                }
            }

            for (String line : Files.readAllLines(Path.of("data/requests.txt"))) {
                if (!line.isBlank()) {
                    requests.add(AccessRequest.fromLine(line));
                }
            }

            for (String line : Files.readAllLines(Path.of("data/grants.txt"))) {
                if (!line.isBlank()) {
                    grants.add(TimeGrant.fromLine(line));
                }
            }

            AccountService accounts = new AccountService();
            accounts.restore(students, requests, grants);

            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Administrator");
                frame.add(new AdminPanel(
                        accounts,
                        () -> LocalDateTime.of(2026, 11, 16, 8, 50)
                ));
                frame.setSize(900, 450);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            });
        } catch (IOException | RuleException e) {
            JOptionPane.showMessageDialog(
                    null, "Could not load data: " + e.getMessage());
        }
    }
}