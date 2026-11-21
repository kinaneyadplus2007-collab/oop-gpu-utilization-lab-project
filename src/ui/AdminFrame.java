package ui;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import logic.*;
import model.RuleException;

public class AdminFrame extends JFrame implements Refreshable {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext context;
    private final Runnable logoutAction;
    private final JLabel clockLabel = new JLabel("Simulation time: not set");
    private final JButton advanceButton = new JButton("+1 minute");

    public AdminFrame(AppContext context, Runnable logoutAction, JPanel requestsPanel, JPanel studentsPanel) {
        super("GPU Utilization Dashboard · Administrator");
        if (context == null) {
            throw new IllegalArgumentException("AppContext is required.");
        }

        this.context = context;
        this.logoutAction = logoutAction == null ? () -> dispose() : logoutAction;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        header.add(clockLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton logout = new JButton("Log out");
        actions.add(advanceButton);
        actions.add(logout);
        header.add(actions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        DashboardPanel dashboard = new DashboardPanel(context);
        AllJobsPanel allJobs = new AllJobsPanel(context);

        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Requests", requestsPanel == null ? new JPanel() : requestsPanel);
        tabs.addTab("Students", studentsPanel == null ? new JPanel() : studentsPanel);
        tabs.addTab("All jobs", allJobs);
        add(tabs, BorderLayout.CENTER);

        if (requestsPanel instanceof Refreshable) {
            context.register((Refreshable) requestsPanel);
        }
        if (studentsPanel instanceof Refreshable) {
            context.register((Refreshable) studentsPanel);
        }
        context.register(this);

        advanceButton.addActionListener(e -> advanceOneMinute());
        logout.addActionListener(e -> {
            context.unregister(this);
            this.logoutAction.run();
        });

        setSize(1120, 720);
        setLocationRelativeTo(null);
        refresh();
    }

    public AdminFrame(AppContext context, Runnable logoutAction) {
        this(context, logoutAction, null, null);
    }

    private void advanceOneMinute() {
        try {
            context.getScheduler().advanceMinute();
            context.afterChange();
        } catch (RuleException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Simulation error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not save or refresh the application: " + ex.getMessage(), "Application error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        if (context.getClock().isSet()) {
            clockLabel.setText("Simulated time: " + context.getClock().now().format(DISPLAY_TIME));
            advanceButton.setEnabled(true);
        } else {
            clockLabel.setText("Simulated time: not set");
            advanceButton.setEnabled(false);
        }
    }
}
