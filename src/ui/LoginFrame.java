package ui;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JLabel messageLabel = new JLabel(" ");

    public LoginFrame() {
        super("GPU Utilization Management");

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

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter your username and password.");
            return;
        }

        messageLabel.setText(" ");

        // Connect AccountService.login(...) here when available.
        JOptionPane.showMessageDialog(
                this,
                "Login service is not connected yet.",
                "Login",
                JOptionPane.INFORMATION_MESSAGE
        );
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
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}