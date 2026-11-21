package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

import logic.Scheduler;
import logic.SimulationClock;
import model.RuleException;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;

public class StudentHomePanel extends JPanel {

    private final Student student;
    private final Scheduler scheduler;
    private final SimulationClock clock;
    private final Runnable onChange;

    private final JLabel timeLabel = new JLabel();
    private final JLabel allocatedLabel = new JLabel();
    private final JLabel usedLabel = new JLabel();
    private final JLabel reservedLabel = new JLabel();
    private final JLabel availableLabel = new JLabel();

    private final JTextField titleField = new JTextField(20);
    private final JTextField applicationField = new JTextField(20);
    private final JTextArea descriptionArea = new JTextArea(4, 20);

    private final JComboBox<TaskSize> sizeBox =
            new JComboBox<>(TaskSize.values());

    private final JLabel durationLabel = new JLabel();
    private final JButton submitButton = new JButton("Submit job");

    public StudentHomePanel(Student student, Scheduler scheduler,
                            SimulationClock clock, Runnable onChange) {

        this.student = student;
        this.scheduler = scheduler;
        this.clock = clock;
        this.onChange = onChange;

        setLayout(new BorderLayout(10, 15));
        setBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );

        JPanel summary = new JPanel(new GridLayout(5, 2, 8, 8));

        summary.add(new JLabel("Simulation time:"));
        summary.add(timeLabel);

        summary.add(new JLabel("Allocated minutes:"));
        summary.add(allocatedLabel);

        summary.add(new JLabel("Used minutes:"));
        summary.add(usedLabel);

        summary.add(new JLabel("Reserved minutes:"));
        summary.add(reservedLabel);

        summary.add(new JLabel("Available minutes:"));
        summary.add(availableLabel);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));

        form.add(new JLabel("Job title:"));
        form.add(titleField);

        form.add(new JLabel("Application name:"));
        form.add(applicationField);

        form.add(new JLabel("Description:"));
        form.add(new JScrollPane(descriptionArea));

        form.add(new JLabel("Task size:"));
        form.add(sizeBox);
        form.add(durationLabel);
        form.add(submitButton);

        add(summary, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);

        sizeBox.addActionListener(e -> updateDuration());
        submitButton.addActionListener(e -> submitJob());

        updateDuration();
        refresh();
    }

    private void updateDuration() {
        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        durationLabel.setText(
                size == null
                        ? "Select a task size."
                        : "Duration: " + size.getUnits() + " minutes"
        );
    }

    private void submitJob() {
        String title = titleField.getText().trim();
        String application = applicationField.getText().trim();
        String description = descriptionArea.getText().trim();
        TaskSize size = (TaskSize) sizeBox.getSelectedItem();

        if (title.isEmpty() || application.isEmpty()
                || description.isEmpty() || size == null) {

            JOptionPane.showMessageDialog(
                    this, "Complete all job fields."
            );
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Submit \"" + title + "\"?\n"
                + "Duration: " + size.getUnits() + " GPU minutes.",
                "Confirm submission",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            scheduler.submit(
                    student, title, application, description, size
            );
        } catch (RuleException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Submission failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        titleField.setText("");
        applicationField.setText("");
        descriptionArea.setText("");

        refresh();
        onChange.run();
    }

    public void refresh() {
        TimeAllowance allowance = student.getAllowance();

        allocatedLabel.setText(
                String.valueOf(allowance.getAllocated())
        );
        usedLabel.setText(
                String.valueOf(allowance.getUsed())
        );
        reservedLabel.setText(
                String.valueOf(allowance.getReserved())
        );
        availableLabel.setText(
                String.valueOf(allowance.available())
        );

        if (clock.isSet()) {
            timeLabel.setText(
                    clock.now().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    )
            );
        } else {
            timeLabel.setText("Waiting for administrator to set the clock");
        }

        submitButton.setEnabled(
                clock.isSet() && allowance.available() > 0
        );
    }
}