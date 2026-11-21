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
                    "Wait", "Charged"
            },
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);

    public AllJobsPanel(AppContext context) {
        super(new BorderLayout(6, 6));
        this.context = context;

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Search by job, student, title, or application:"));
        filters.add(search);
        filters.add(statusFilter);
        add(filters, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

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
    }

    @Override
    public void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }

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
                    format(start),
                    format(finish),
                    wait,
                    context.getScheduler().minutesCharged(job) + " min"
            });
        }

        applyFilters();
    }

    private static String format(LocalDateTime time) {
        return time == null ? "—" : time.format(TIME);
    }
}
