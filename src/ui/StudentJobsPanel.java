package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import logic.Scheduler;
import model.Job;
import model.JobStatus;
import model.RuleException;
import model.Student;

public class StudentJobsPanel extends JPanel {

    private final Student student;
    private final Scheduler scheduler;
    private final Runnable onChange;

    private List<Job> jobs = new ArrayList<>();

    private final JTextField searchField = new JTextField(18);

    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[] {
                "All statuses", "QUEUED", "RUNNING",
                "COMPLETED", "CANCELLED"
            }
    );

    private final JButton cancelButton =
            new JButton("Cancel selected job");

    private final DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[] {
                        "ID", "Title", "Application", "Size / duration",
                        "Status", "GPU", "Submitted", "Start", "Finish",
                        "Remaining wait (min)", "Queue wait (min)",
                        "Charged (min)"
                    }, 0
            ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable table = new JTable(tableModel);

    public StudentJobsPanel(Student student,
                            Scheduler scheduler,
                            Runnable onChange) {

        this.student = student;
        this.scheduler = scheduler;
        this.onChange = onChange;

        setLayout(new BorderLayout(8, 8));
        setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel()
                    .getColumn(i).setPreferredWidth(150);
        }

        JButton searchButton = new JButton("Search");
        JButton clearButton = new JButton("Clear");

        JPanel search = new JPanel(
                new FlowLayout(FlowLayout.LEFT)
        );

        search.add(new JLabel("Job ID or title:"));
        search.add(searchField);
        search.add(statusBox);
        search.add(searchButton);
        search.add(clearButton);

        add(search, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        JButton detailsButton = new JButton("View details");

        cancelButton.setEnabled(false);

        JPanel buttons = new JPanel();
        buttons.add(refreshButton);
        buttons.add(detailsButton);
        buttons.add(cancelButton);

        JPanel bottom = new JPanel(new BorderLayout());

        bottom.add(new JLabel(
                "Only your jobs. ≈ = predicted. "
                + "Queue wait is time before execution."
        ), BorderLayout.NORTH);

        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        statusBox.addActionListener(e -> refresh());

        clearButton.addActionListener(e -> {
            searchField.setText("");
            statusBox.setSelectedIndex(0);
            refresh();
        });

        refreshButton.addActionListener(e -> refresh());
        detailsButton.addActionListener(e -> showDetails());
        cancelButton.addActionListener(e -> cancelSelectedJob());

        table.getSelectionModel().addListSelectionListener(e -> {
            Job job = selectedJob();

            cancelButton.setEnabled(
                    job != null && job.isCancellable()
            );
        });

        refresh();
    }

    public void refresh() {
        table.clearSelection();
        tableModel.setRowCount(0);
        jobs = new ArrayList<>();
        cancelButton.setEnabled(false);

        String query = searchField.getText()
                .trim().toLowerCase(Locale.ROOT);

        String selectedStatus =
                (String) statusBox.getSelectedItem();

        for (Job job : scheduler.getJobsOf(student.getUsername())) {

            boolean matchesText =
                    job.getJobId().toLowerCase(Locale.ROOT)
                            .contains(query)
                    || job.getTitle().toLowerCase(Locale.ROOT)
                            .contains(query);

            boolean matchesStatus =
                    "All statuses".equals(selectedStatus)
                    || job.getStatus().name().equals(selectedStatus);

            if (!matchesText || !matchesStatus) {
                continue;
            }

            jobs.add(job);

            boolean queued =
                    job.getStatus() == JobStatus.QUEUED;

            String status = queued
                    ? "Queued #" + scheduler.queuePosition(job)
                    : job.getStatus().toString();

            String gpu = job.getAssignedGpuId() > 0
                    ? String.valueOf(job.getAssignedGpuId())
                    : "-";

            String start = formatTime(
                    scheduler.predictedStart(job), queued
            );

            String finish = formatTime(
                    scheduler.predictedFinish(job),
                    queued || job.getStatus() == JobStatus.RUNNING
            );

            tableModel.addRow(new Object[] {
                job.getJobId(),
                job.getTitle(),
                job.getApplicationName(),
                job.getTaskSize().getLabel()
                        + " (" + job.getDuration() + " min)",
                status,
                gpu,
                formatTime(job.getSubmissionTime(), false),
                start,
                finish,
                queued ? scheduler.remainingWait(job) : "-",
                job.getStartTime() == null
                        ? "-" : scheduler.queueWait(job),
                job.isSettled()
                        ? scheduler.minutesCharged(job) : "-"
            });
        }
    }

    private String formatTime(LocalDateTime time,
                              boolean estimated) {

        if (time == null) {
            return "-";
        }

        return (estimated ? "≈ " : "") + time.format(timeFormat);
    }

    private Job selectedJob() {
        int row = table.getSelectedRow();

        return row == -1 ? null
                : jobs.get(table.convertRowIndexToModel(row));
    }

    private String detailsText(Job job) {
        String text =
                "Job ID: " + job.getJobId()
                + "\nTitle: " + job.getTitle()
                + "\nApplication: " + job.getApplicationName()
                + "\nDescription: " + job.getDescription()
                + "\nTask size: " + job.getTaskSize().getLabel()
                + "\nDuration: " + job.getDuration() + " min"
                + "\nStatus: " + job.getStatus()
                + "\nSubmitted: "
                + formatTime(job.getSubmissionTime(), false)
                + "\nRuntime so far: "
                + scheduler.minutesCharged(job) + " min"
                + "\nCharged: "
                + (job.isSettled()
                        ? scheduler.minutesCharged(job) + " min"
                        : "Not settled yet");

        if (job.getStatus() == JobStatus.CANCELLED) {
            text += "\nCancelled by: " + job.getCancelledBy()
                    + "\nCancelled at: "
                    + formatTime(job.getCancelTime(), false)
                    + "\nReason: " + job.getCancelReason();
        }

        return text;
    }

    private void showDetails() {
        Job job = selectedJob();

        if (job == null) {
            JOptionPane.showMessageDialog(
                    this, "Select a job first."
            );
            return;
        }

        JTextArea details = new JTextArea(
                detailsText(job), 18, 50
        );

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setCaretPosition(0);

        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(details),
                "Job details",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void cancelSelectedJob() {
        Job job = selectedJob();

        if (job == null) {
            JOptionPane.showMessageDialog(
                    this, "Select a job first."
            );
            return;
        }

        if (!job.isCancellable()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Only queued or running jobs can be cancelled."
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

        int charge = scheduler.minutesCharged(job);
        int returned = job.getDuration() - charge;

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel job " + job.getJobId() + "?"
                + "\nTotal charge for this job: "
                + charge + " GPU minutes."
                + "\nReturned to available balance: "
                + returned + " minutes.",
                "Confirm cancellation",
                JOptionPane.YES_NO_OPTION
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            scheduler.cancel(job, student, reason.trim());

        } catch (RuleException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Cancellation failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        refresh();

        if (onChange != null) {
            onChange.run();
        }
    }
}