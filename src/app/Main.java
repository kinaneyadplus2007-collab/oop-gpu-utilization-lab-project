package app;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.*;

import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;
import model.Administrator;
import model.RuleException;
import model.Student;
import model.User;
import storage.DataStore;
import ui.AdminPanel;
import ui.ClockSetupDialog;
import ui.LoginFrame;
import ui.StudentFrame;

public class Main {

    private final AccountService accounts = new AccountService();
    private final SimulationClock clock = new SimulationClock();
    private final Scheduler scheduler = new Scheduler(clock, accounts);
    private final DataStore store = new DataStore();

    private LoginFrame loginFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        if (!Files.isDirectory(Path.of("data"))) {
            JOptionPane.showMessageDialog(
                    null,
                    "Run the application from the project folder containing data/.",
                    "Data folder not found",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            store.loadAll(accounts, scheduler, clock);

        } catch (RuleException e) {
            JOptionPane.showMessageDialog(
                    null,
                    e.getMessage(),
                    "Could not load saved data",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        loginFrame = new LoginFrame(
                accounts,
                user -> openUserWindow(user),
                () -> save()
        );

        loginFrame.setDefaultCloseOperation(
                JFrame.DO_NOTHING_ON_CLOSE
        );

        loginFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (save()) {
                    loginFrame.dispose();
                }
            }
        });

        loginFrame.setVisible(true);
    }

    private void openUserWindow(User user) {
        JFrame window;

        if (user instanceof Student) {
            window = new StudentFrame(
                    (Student) user,
                    scheduler,
                    clock,
                    () -> save()
            );

        } else if (user instanceof Administrator) {
            window = new JFrame(
                    "GPU Management — Administrator"
            );

            if (!clock.isSet()) {
                new ClockSetupDialog(window, clock)
                        .setVisible(true);

                if (!clock.isSet() || !save()) {
                    window.dispose();
                    return;
                }
            }

            window.setContentPane(
                    new AdminPanel(accounts, () -> clock.now())
            );

            window.setSize(900, 550);
            window.setLocationRelativeTo(null);

        } else {
            JOptionPane.showMessageDialog(
                    loginFrame, "Unknown user role."
            );
            return;
        }

        window.setDefaultCloseOperation(
                JFrame.DO_NOTHING_ON_CLOSE
        );

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (save()) {
                    window.dispose();
                    loginFrame.setVisible(true);
                }
            }
        });

        JButton logout = new JButton("Log out");

        logout.addActionListener(e -> window.dispatchEvent(
                new WindowEvent(
                        window,
                        WindowEvent.WINDOW_CLOSING
                )
        ));

        JPanel buttons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        buttons.add(logout);

        JPanel content = new JPanel(new BorderLayout());
        content.add(
                window.getContentPane(),
                BorderLayout.CENTER
        );
        content.add(buttons, BorderLayout.NORTH);

        window.setContentPane(content);

        loginFrame.setVisible(false);
        window.setVisible(true);
    }

    private boolean save() {
        try {
            store.saveAll(accounts, scheduler, clock);
            return true;

        } catch (RuleException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Changes are still in memory, but could not be saved.\n"
                    + e.getMessage(),
                    "Save failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
}