package model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Job implements Storable, Cancellable {
    private final String jobId;
    private final String studentId;
    private final String title;
    private final String applicationName;
    private final String description;
    private final TaskSize taskSize;
    private final LocalDateTime submissionTime;

    private JobStatus status = JobStatus.QUEUED;
    private int assignedGpuId; // 0 means no GPU assigned yet
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String cancelledBy;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private boolean settled;

    public Job(String jobId, String studentId, String title, String applicationName,
               String description, TaskSize taskSize, LocalDateTime submissionTime) {
        this.jobId = jobId;
        this.studentId = studentId;
        this.title = title;
        this.applicationName = applicationName;
        this.description = description;
        this.taskSize = taskSize;
        this.submissionTime = submissionTime;
    }

    public String getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public TaskSize getTaskSize() { return taskSize; }
    public int getWorkloadUnits() { return taskSize.getUnits(); }
    public int getDuration() { return taskSize.getUnits(); }
    public LocalDateTime getSubmissionTime() { return submissionTime; }

    public JobStatus getStatus() { return status; }
    public int getAssignedGpuId() { return assignedGpuId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public String getCancelledBy() { return cancelledBy; }
    public String getCancelReason() { return cancelReason; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public boolean isSettled() { return settled; }

    public void markSettled() {
        settled = true;
    }

    public void start(int gpuId, LocalDateTime now) throws RuleException {
        if (status != JobStatus.QUEUED) {
            throw new RuleException("Job " + jobId + " cannot start: it is " + status + ".");
        }
        assignedGpuId = gpuId;
        startTime = now;
        status = JobStatus.RUNNING;
    }

    public void complete(LocalDateTime now) throws RuleException {
        if (status != JobStatus.RUNNING) {
            throw new RuleException("Job " + jobId + " cannot complete: it is " + status + ".");
        }
        finishTime = now;
        status = JobStatus.COMPLETED;
    }

    @Override
    public boolean isCancellable() {
        return status == JobStatus.QUEUED || status == JobStatus.RUNNING;
    }

    public void cancel(String by, String reason, LocalDateTime now) throws RuleException {
        if (!isCancellable()) {
            throw new RuleException("Job " + jobId + " is " + status + " and cannot be cancelled.");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuleException("A reason is required to cancel a job.");
        }
        if (status == JobStatus.RUNNING) {
            finishTime = now;
        }
        cancelledBy = by;
        cancelReason = reason.trim();
        cancelTime = now;
        status = JobStatus.CANCELLED;
    }

    public int minutesRun(LocalDateTime now) {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime end = (finishTime != null) ? finishTime : now;
        return (int) ChronoUnit.MINUTES.between(startTime, end);
    }

    public LocalDateTime expectedFinish() {
        return (startTime == null) ? null : startTime.plusMinutes(getDuration());
    }

    @Override
    public String toLine() {
        return Lines.join(
                Lines.enc(jobId),
                Lines.enc(studentId),
                Lines.enc(title),
                Lines.enc(applicationName),
                Lines.enc(description),
                taskSize.name(),
                Lines.time(submissionTime),
                status.name(),
                String.valueOf(assignedGpuId),
                Lines.time(startTime),
                Lines.time(finishTime),
                Lines.enc(cancelledBy),
                Lines.enc(cancelReason),
                Lines.time(cancelTime),
                String.valueOf(settled)
        );
    }

    public static Job fromLine(String line) throws RuleException {
        String[] f = Lines.split(line, 15);

        try {
            Job job = new Job(
                    Lines.dec(f[0]),
                    Lines.dec(f[1]),
                    Lines.dec(f[2]),
                    Lines.dec(f[3]),
                    Lines.dec(f[4]),
                    TaskSize.valueOf(f[5]),
                    Lines.parseTime(f[6])
            );

            job.status = JobStatus.valueOf(f[7]);
            job.assignedGpuId = Integer.parseInt(f[8]);
            job.startTime = Lines.parseTime(f[9]);
            job.finishTime = Lines.parseTime(f[10]);
            job.cancelledBy = f[11].isEmpty() ? null : Lines.dec(f[11]);
            job.cancelReason = f[12].isEmpty() ? null : Lines.dec(f[12]);
            job.cancelTime = Lines.parseTime(f[13]);
            job.settled = Boolean.parseBoolean(f[14]);

            return job;
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            throw new RuleException("Bad saved job line.");
        }
    }
}