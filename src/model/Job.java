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
    private int assignedGpuId;   // 0 means that no gpu yet     
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
        throw new UnsupportedOperationException("TODO: agree the line format with Ali (DataStore)");
    }
}