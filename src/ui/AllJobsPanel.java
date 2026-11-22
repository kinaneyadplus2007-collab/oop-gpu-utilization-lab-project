package ui;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import model.*;

public class AllJobsPanel extends JPanel implements Refreshable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppContext context;
    private final JTextField search = new JTextField(24);
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[] {"All statuses", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"});

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {
                    "Job", "Student", "Title", "Application", "Size", "Status",
                    "GPU", "Submitted", "Start / predicted", "Finish / predicted",
                    "Wait", "Charged", "Queue position"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JTable table = new JTable(model);
    private final JButton cancelButton = new JButton("Cancel selected job");
    private final JButton detailsButton = new JButton("View details");

    public AllJobsPanel(AppContext context) {
        super(new BorderLayout(6, 6));
        this.context = context;

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search by job, student, title, or application:"));
        filters.add(search);
        filters.add(statusFilter);
        add(filters, BorderLayout.NORTH);

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(
                    i >= 7 && i <= 9 ? 170 : 120
            );
        }
        add(new JScrollPane(table), BorderLayout.CENTER);

        cancelButton.setEnabled(false);
        detailsButton.setEnabled(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(detailsButton);
        actions.add(cancelButton);
        add(actions, BorderLayout.SOUTH);

        cancelButton.addActionListener(e -> cancelSelectedJob());
        detailsButton.addActionListener(e -> showDetails());
        table.getSelectionModel().addListSelectionListener(
                e -> updateCancelButton()
        );

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });

        statusFilter.addActionListener(e -> applyFilters());
        context.register(this);
        refresh();
    }

    private void applyFilters() {
        String text = search.getText().trim().toLowerCase(java.util.Locale.ROOT);
        String selected = (String) statusFilter.getSelectedItem();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (!"All statuses".equals(selected) && !selected.equals(entry.getStringValue(5))) {
                    return false;
                }

                if (text.isEmpty()) {
                    return true;
                }

                for (int column = 0; column <= 3; column++) {
                    if (entry.getStringValue(column).toLowerCase(java.util.Locale.ROOT).contains(text)) {
                        return true;
                    }
                }
                return false;
            }
        });
        updateCancelButton();
    }

    private Job selectedJob() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= table.getRowCount()) {
            return null;
        }

        int modelRow = table.convertRowIndexToModel(row);
        String id = model.getValueAt(modelRow, 0).toString();
        return context.getScheduler().findJob(id);
    }

    private void updateCancelButton() {
        Job job = selectedJob();
        cancelButton.setEnabled(job != null && job.isCancellable());
        detailsButton.setEnabled(job != null);
    }

    private String detailsText(Job job) {
        boolean queued = job.getStatus() == JobStatus.QUEUED;
        boolean running = job.getStatus() == JobStatus.RUNNING;

        String text = "Job: " + job.getJobId()
                + "\nStudent: " + job.getStudentId()
                + "\nTitle: " + job.getTitle()
                + "\nApplication: " + job.getApplicationName()
                + "\nDescription: " + job.getDescription()
                + "\nTask size: " + job.getTaskSize().getLabel()
                + "\nWorkload: " + job.getWorkloadUnits() + " units"
                + "\nDuration: " + job.getDuration() + " min"
                + "\nStatus: " + job.getStatus()
                + "\nGPU: " + (job.getAssignedGpuId() == 0 ? "—" : job.getAssignedGpuId())
                + "\nSubmitted: " + format(job.getSubmissionTime())
                + "\nStart / predicted: " + format(context.getScheduler().predictedStart(job), queued)
                + "\nFinish / predicted: " + format(context.getScheduler().predictedFinish(job), queued || running)
                + "\nQueue position: " + (queued ? context.getScheduler().queuePosition(job) : "—")
                + "\nQueue wait so far / total: " + context.getScheduler().queueWait(job) + " min"
                + (queued ? "\nRemaining wait: " + context.getScheduler().remainingWait(job) + " min" : "")
                + "\nRuntime so far / total: " + context.getScheduler().minutesCharged(job) + " min"
                + "\nCharged: " + (job.isSettled()
                        ? context.getScheduler().minutesCharged(job) + " min"
                        : "Not settled yet");

        if (job.getStatus() == JobStatus.CANCELLED) {
            text += "\nCancelled by: " + job.getCancelledBy()
                    + "\nCancelled at: " + format(job.getCancelTime())
                    + "\nReason: " + job.getCancelReason();
        }
        return text;
    }

    private void showDetails() {
        Job job = selectedJob();
        if (job == null) return;

        JTextArea details = new JTextArea(detailsText(job), 22, 55);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setCaretPosition(0);

        JOptionPane.showMessageDialog(
                this, new JScrollPane(details), "Job details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void cancelSelectedJob() {
        Job job = selectedJob();
        if (job == null || !job.isCancellable()) {
            JOptionPane.showMessageDialog(
                    this, "Select a queued or running job."
            );
            return;
        }

        String reason = JOptionPane.showInputDialog(
                this, "Enter the cancellation reason:"
        );
        if (reason == null) {
            return;
        }
        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "A cancellation reason is required."
            );
            return;
        }

        int charge = context.getScheduler().minutesCharged(job);
        int returned = job.getDuration() - charge;

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel job " + job.getJobId() + "?"
                + "\nStudent: " + job.getStudentId()
                + "\nTotal charge: " + charge + " GPU minutes."
                + "\nReturned to available balance: " + returned + " minutes.",
                "Confirm cancellation",
                JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            context.getScheduler().cancel(
                    job, context.getAccounts().getAdmin(), reason.trim()
            );
        } catch (RuleException ex) {
            JOptionPane.showMessageDialog(
                    this, ex.getMessage(), "Cancellation failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        context.afterChange();
    }

    @Override
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

        table.clearSelection();
        model.setRowCount(0);
        for (Job job : context.getScheduler().getAllJobs()) {
            LocalDateTime start = context.getScheduler().predictedStart(job);
            LocalDateTime finish = context.getScheduler().predictedFinish(job);
            String gpu = job.getAssignedGpuId() == 0 ? "—" : "GPU " + job.getAssignedGpuId();
            String wait = job.getStatus() == JobStatus.QUEUED ? context.getScheduler().remainingWait(job) + " min" : context.getScheduler().queueWait(job) + " min";

            model.addRow(new Object[] {
                    job.getJobId(),
                    job.getStudentId(),
                    job.getTitle(),
                    job.getApplicationName(),
                    job.getTaskSize().getLabel() + " (" + job.getDuration() + ")",
                    job.getStatus(),
                    gpu,
                    format(job.getSubmissionTime()),
                    format(start, job.getStatus() == JobStatus.QUEUED),
                    format(finish, job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING),
                    wait,
                    job.isSettled() ? context.getScheduler().minutesCharged(job) + " min" : "—",
                    job.getStatus() == JobStatus.QUEUED ? context.getScheduler().queuePosition(job) : "—"
            });
        }

        applyFilters();
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : time.format(TIME);
    }

    private static String format(LocalDateTime time, boolean predicted) {
        return time == null ? "—" : (predicted ? "≈ " : "") + time.format(TIME);
    }
}
