package ui;

import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    private final DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DefaultTableModel tableModel =
            new DefaultTableModel(
                    new String[] {
                        "ID", "Title", "Status", "GPU",
                        "Duration (min)", "Start", "Finish",
                        "Remaining wait (min)"
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
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );

        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        JButton cancelButton = new JButton("Cancel selected job");

        JPanel buttons = new JPanel();
        buttons.add(refreshButton);
        buttons.add(cancelButton);

        add(buttons, BorderLayout.SOUTH);

        refreshButton.addActionListener(e -> refresh());
        cancelButton.addActionListener(e -> cancelSelectedJob());

        refresh();
    }

    public void refresh() {
        jobs = scheduler.getJobsOf(student.getUsername());
        tableModel.setRowCount(0);

        for (Job job : jobs) {
            boolean queued = job.getStatus() == JobStatus.QUEUED;

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
                status,
                gpu,
                job.getDuration(),
                start,
                finish,
                queued ? scheduler.remainingWait(job) : 0
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

    private void cancelSelectedJob() {
        int row = table.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this, "Select a job first."
            );
            return;
        }

        Job job = jobs.get(table.convertRowIndexToModel(row));

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

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel job " + job.getJobId() + "?"
                + "\nTotal charge for this job: "
                + charge + " GPU minutes.",
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