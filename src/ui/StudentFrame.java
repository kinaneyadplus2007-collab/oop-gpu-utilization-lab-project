package ui;

import javax.swing.*;

import logic.Scheduler;
import logic.SimulationClock;
import model.Student;

public class StudentFrame extends JFrame {

    private final StudentHomePanel homePanel;
    private final StudentJobsPanel jobsPanel;

    private final Runnable onChange;

    public StudentFrame(Student student,
                        Scheduler scheduler,
                        SimulationClock clock,
                        Runnable onChange) {

        super("GPU Management — " + student.getName());

        this.onChange = onChange;

        homePanel = new StudentHomePanel(
                student, scheduler, clock, () -> dataChanged()
        );

        jobsPanel = new StudentJobsPanel(
                student, scheduler, () -> dataChanged()
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Home", homePanel);
        tabs.addTab("My Jobs", jobsPanel);

        tabs.addChangeListener(e -> refresh());

        setContentPane(tabs);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    public void refresh() {
        homePanel.refresh();
        jobsPanel.refresh();
    }

    private void dataChanged() {
        refresh();

        if (onChange != null) {
            onChange.run();
        }
    }
}