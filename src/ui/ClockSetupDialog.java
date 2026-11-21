package ui;

import java.awt.GridLayout;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import javax.swing.*;

import logic.SimulationClock;
import model.RuleException;

public class ClockSetupDialog extends JDialog {

    private final SimulationClock clock;

    private final JSpinner dateSpinner =
            new JSpinner(new SpinnerDateModel());

    private final JSpinner timeSpinner =
            new JSpinner(new SpinnerDateModel());

    public ClockSetupDialog(JFrame owner, SimulationClock clock) {
        super(owner, "Start the simulation", true);

        this.clock = clock;

        dateSpinner.setEditor(
                new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy")
        );

        timeSpinner.setEditor(
                new JSpinner.DateEditor(timeSpinner, "HH:mm")
        );

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );

        panel.add(new JLabel("Starting date:"));
        panel.add(dateSpinner);

        panel.add(new JLabel("Starting time:"));
        panel.add(timeSpinner);

        JButton startButton = new JButton("Start clock");
        JButton cancelButton = new JButton("Cancel");

        panel.add(startButton);
        panel.add(cancelButton);

        startButton.addActionListener(e -> startClock());
        cancelButton.addActionListener(e -> dispose());

        setContentPane(panel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void startClock() {
        try {
            dateSpinner.commitEdit();
            timeSpinner.commitEdit();

            Date selectedDate = (Date) dateSpinner.getValue();
            Date selectedTime = (Date) timeSpinner.getValue();

            ZoneId zone = ZoneId.systemDefault();

            LocalDate date = selectedDate.toInstant()
                    .atZone(zone)
                    .toLocalDate();

            LocalTime time = selectedTime.toInstant()
                    .atZone(zone)
                    .toLocalTime()
                    .withSecond(0)
                    .withNano(0);

            LocalDateTime start = LocalDateTime.of(date, time);

            clock.set(start);
            dispose();

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a valid date and time.",
                    "Invalid input",
                    JOptionPane.ERROR_MESSAGE
            );

        } catch (RuleException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Clock setup",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}