package ui;

import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.*;
import logic.AccountService;
import logic.Validator;
import model.RuleException;

public class RegisterPanel extends JPanel {

    private final JTextField idField = new JTextField(20);
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    private final JLabel idError = new JLabel(" ");
    private final JLabel nameError = new JLabel(" ");
    private final JLabel emailError = new JLabel(" ");
    private final JLabel phoneError = new JLabel(" ");
    private final JLabel passwordError = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ");

    private final AccountService accounts;
    private final Runnable backToLogin;

    public RegisterPanel(AccountService accounts, Runnable backToLogin) {
        this.accounts = accounts;
        this.backToLogin = backToLogin;

        setLayout(new GridLayout(0, 1, 6, 4));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(new JLabel("Request a student account"));
        add(new JLabel("Approval is required before you can sign in."));

        addField("University ID", idField, idError);
        addField("Full name", nameField, nameError);
        addField("Email", emailField, emailError);
        addField("Phone number", phoneField, phoneError);
        addField("Password", passwordField, passwordError);

        JButton submit = new JButton("Submit request");
        JButton back = new JButton("Back to login");

        messageLabel.setForeground(Color.RED);

        add(submit);
        add(back);
        add(messageLabel);

        submit.addActionListener(e -> submitRequest());
        back.addActionListener(e -> backToLogin.run());
    }

    private void addField(String label, JTextField field, JLabel error) {
        error.setForeground(Color.RED);

        add(new JLabel(label));
        add(field);
        add(error);
    }

    private void submitRequest() {
        idError.setText(" ");
        nameError.setText(" ");
        emailError.setText(" ");
        phoneError.setText(" ");
        passwordError.setText(" ");
        messageLabel.setText(" ");

        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean valid = true;

        if (!Validator.notBlank(id)) {
            idError.setText("Enter your university ID.");
            valid = false;
        }

        if (!Validator.notBlank(name)) {
            nameError.setText("Enter your full name.");
            valid = false;
        }

        if (!Validator.validEmail(email)) {
            emailError.setText("Enter a valid email address.");
            valid = false;
        }

        if (!Validator.validPhone(phone)) {
            phoneError.setText("Enter a phone number with 7 to 15 digits.");
            valid = false;
        }

        if (!Validator.notBlank(password)) {
            passwordError.setText("Enter a password.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        try {
            accounts.register(id, name, email, phone, password);

            passwordField.setText("");

            JOptionPane.showMessageDialog(
                    this,
                    "Request sent. Status: Pending.",
                    "Registration",
                    JOptionPane.INFORMATION_MESSAGE
            );

            backToLogin.run();

        } catch (RuleException e) {
            messageLabel.setText(e.getMessage());
        }
    }
}
