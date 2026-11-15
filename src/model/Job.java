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
    private int assignedGpuId;              // 0 = no GPU yet
    private LocalDateTime startTime;        // null until the job starts running
    private LocalDateTime finishTime;       // completion time, or the time a running job was cancelled
    private String cancelledBy;
    private String cancelReason;
    private LocalDateTime cancelTime;
    private boolean settled;                // true once the allowance was settled (never charge twice)

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

    // ----- fields that never change after the job is accepted -----

    public String getJobId() { return jobId; }
    public String getStudentId() { return studentId; }
    public String getTitle() { return title; }
    public String getApplicationName() { return applicationName; }
    public String getDescription() { return description; }
    public TaskSize getTaskSize() { return taskSize; }
    public int getWorkloadUnits() { return taskSize.getUnits(); }
    public int getDuration() { return taskSize.getUnits(); }
    public LocalDateTime getSubmissionTime() { return submissionTime; }

    // ----- fields set while the job moves through its states -----

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

    // ----- state changes -----

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

    @Override
    public void cancel(String by, String reason, LocalDateTime now) throws RuleException {
        if (!isCancellable()) {
            throw new RuleException("Job " + jobId + " is " + status + " and cannot be cancelled.");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuleException("A reason is required to cancel a job.");
        }
        if (status == JobStatus.RUNNING) {
            finishTime = now;               // the time a running job was cancelled
        }
        cancelledBy = by;
        cancelReason = reason.trim();
        cancelTime = now;
        status = JobStatus.CANCELLED;
    }

    // ----- values worked out from the records -----

    /** Minutes this job has really run on a GPU (0 if it never started). */
    public int minutesRun(LocalDateTime now) {
        if (startTime == null) {
            return 0;
        }
        LocalDateTime end = (finishTime != null) ? finishTime : now;
        return (int) ChronoUnit.MINUTES.between(startTime, end);
    }

    /** When a running job will finish (start + duration), or null if it has not started. */
    public LocalDateTime expectedFinish() {
        return (startTime == null) ? null : startTime.plusMinutes(getDuration());
    }

    @Override
    public String toLine() {
        throw new UnsupportedOperationException("TODO: agree the line format with Ali (DataStore)");
    }
}