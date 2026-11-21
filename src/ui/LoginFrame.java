package ui;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.*;
import logic.AccountService;
import model.RuleException;
import model.User;

public class LoginFrame extends JFrame {

    private final AccountService accounts;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame(AccountService accounts) {
        super("GPU Utilization Management");
        this.accounts = accounts;
        messageLabel.setForeground(Color.RED);

        JButton signIn = new JButton("Sign in");
        JButton register = new JButton("New student? Request an account");

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        form.add(new JLabel("GPU Utilization Management"));
        form.add(new JLabel("4 × NVIDIA H100 — simulated"));
        form.add(new JLabel("Username (university ID)"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        form.add(signIn);
        form.add(register);
        form.add(messageLabel);

        setContentPane(form);

        signIn.addActionListener(e -> signIn());
        register.addActionListener(e -> openRegistration());

        getRootPane().setDefaultButton(signIn);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    private void signIn() {
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());

    try {
        User user = accounts.login(username, password);

        messageLabel.setText(" ");
        passwordField.setText("");

        JOptionPane.showMessageDialog(
                this,
                "Welcome, " + user.getName()
                + " (" + user.getRoleLabel() + ")"
        );

    } catch (RuleException e) {
        messageLabel.setText(e.getMessage());
    }
}

    private void openRegistration() {
        // Open RegisterPanel here once we build it.
        JOptionPane.showMessageDialog(
                this,
                "Registration form is not connected yet.",
                "Registration",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
    AccountService accounts = new AccountService();

    SwingUtilities.invokeLater(() -> {
        new LoginFrame(accounts).setVisible(true);
    });
}
}