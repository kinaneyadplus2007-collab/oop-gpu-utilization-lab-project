package ui;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import logic.*;
import model.*;

public class DashboardPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext context;
    private final JLabel summary = new JLabel();
    private final JPanel gpuGrid = new JPanel(new GridLayout(1, 4, 8, 8));
    private final DefaultTableModel waitingModel = model("Job", "Student", "Application", "Duration", "Position", "Predicted start", "Predicted finish", "Wait");
    private final DefaultTableModel completedModel = model("Job", "Student", "GPU", "Submitted", "Started", "Finished","Runtime", "Queue wait", "Charged");
    private final DefaultTableModel balancesModel = model("Student", "Allocated", "Used", "Reserved", "Available");

    public DashboardPanel(AppContext context) {
        super(new BorderLayout(8, 8));
        this.context = context;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(summary, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        gpuGrid.setBorder(BorderFactory.createTitledBorder("GPUs"));
        center.add(gpuGrid, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(3, 1, 6, 6));
        tables.add(tablePanel("Waiting jobs", waitingModel));
        tables.add(tablePanel("Completed jobs", completedModel));
        tables.add(tablePanel("Student allowances", balancesModel));
        center.add(tables, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        context.register(this);
        refresh();
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JPanel tablePanel(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(145);
        }
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        Scheduler scheduler = context.getScheduler();
        summary.setText("Queued: " + scheduler.count(JobStatus.QUEUED)
                + "     Running: " + scheduler.count(JobStatus.RUNNING)
                + "     Completed: " + scheduler.count(JobStatus.COMPLETED)
                + "     Cancelled: " + scheduler.count(JobStatus.CANCELLED)
                + "     Busy GPUs: " + scheduler.busyGpuCount() + "/4"
                + "     Utilization: " + scheduler.utilizationText());

        gpuGrid.removeAll();
        for (GPU gpu : scheduler.getGpus()) {
            Job job = gpu.getCurrentJob();
            JPanel card = new JPanel(new GridLayout(0, 1, 2, 2));
            card.setBorder(BorderFactory.createEtchedBorder());
            card.add(new JLabel("GPU " + gpu.getId()
                    + (job == null ? " · Free" : " · Busy")));

            if (job == null) {
                card.add(new JLabel("No active job"));
            } else {
                card.add(new JLabel("Current: " + job.getJobId() + " · " + job.getApplicationName()));
                card.add(new JLabel("Remaining: " + Math.max(0, job.getDuration() - job.minutesRun(context.getClock().now())) + " min"));
                card.add(new JLabel("Finishes: " + format(job.expectedFinish())));
            }

            gpuGrid.add(card);
        }
        gpuGrid.revalidate();
        gpuGrid.repaint();

        clear(waitingModel);
        for (Job job : scheduler.getQueue()) {
            waitingModel.addRow(new Object[] {
                    job.getJobId(),
                    job.getStudentId(),
                    job.getApplicationName(),
                    job.getDuration() + " min",
                    scheduler.queuePosition(job),
                    "≈ " + format(scheduler.predictedStart(job)),
                    "≈ " + format(scheduler.predictedFinish(job)),
                    scheduler.remainingWait(job) + " min"
            });
        }

        clear(completedModel);
        for (Job job : scheduler.getAllJobs()) {
            if (job.getStatus() == JobStatus.COMPLETED) {
                completedModel.addRow(new Object[] {
                        job.getJobId(),
                        job.getStudentId(),
                        "GPU " + job.getAssignedGpuId(),
                        format(job.getSubmissionTime()),
                        format(job.getStartTime()),
                        format(job.getFinishTime()),
                        scheduler.minutesCharged(job) + " min",
                        scheduler.queueWait(job) + " min",
                        scheduler.minutesCharged(job) + " min"
                });
            }
        }

        clear(balancesModel);
        for (Student student : context.getAccounts().getStudents()) {
            balancesModel.addRow(new Object[] {
                    student.getUsername(),
                    student.getAllowance().getAllocated(),
                    student.getAllowance().getUsed(),
                    student.getAllowance().getReserved(),
                    student.getAllowance().available()
            });
        }
    }

    private static void clear(DefaultTableModel model) {
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : time.format(TIME);
    }
}
