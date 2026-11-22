package ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import model.RuleException;

public class AdminFrame extends JFrame implements Refreshable {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext context;
    private final Runnable logoutAction;
    private final JLabel clockLabel = new JLabel("Simulation time: not set");
    private final JButton advanceButton = new JButton("+1 minute");
    private final DashboardPanel dashboard;
    private final AllJobsPanel allJobs;
    private final JPanel requestsPanel;
    private final JPanel studentsPanel;

    public AdminFrame(AppContext context, Runnable logoutAction, JPanel requestsPanel, JPanel studentsPanel) {
        super("GPU Utilization Dashboard · Administrator");
        if (context == null) {
            throw new IllegalArgumentException("AppContext is required.");
        }

        this.context = context;
        this.logoutAction = logoutAction == null ? () -> dispose() : logoutAction;
        this.requestsPanel = requestsPanel;
        this.studentsPanel = studentsPanel;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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
        dashboard = new DashboardPanel(context);
        allJobs = new AllJobsPanel(context);

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
        logout.addActionListener(e -> this.logoutAction.run());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AdminFrame.this.logoutAction.run();
            }
        });

        setSize(1120, 720);
        setLocationRelativeTo(null);
        refresh();
    }

    public AdminFrame(AppContext context, Runnable logoutAction) {
        this(context, logoutAction, null, null);
    }

    @Override
    public void dispose() {
        context.unregister(this);
        context.unregister(dashboard);
        context.unregister(allJobs);
        if (requestsPanel instanceof Refreshable) {
            context.unregister((Refreshable) requestsPanel);
        }
        if (studentsPanel instanceof Refreshable) {
            context.unregister((Refreshable) studentsPanel);
        }
        super.dispose();
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
