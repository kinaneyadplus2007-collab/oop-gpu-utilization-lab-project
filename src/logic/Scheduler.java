package logic;

import java.time.*;
import java.time.temporal.*;
import java.util.*;
import model.*;

public class Scheduler {
    private static final int GPU_COUNT = 4;

    private final SimulationClock clock;
    private final AccountService accounts;
    private final List<Job> allJobs = new ArrayList<>();
    private final LinkedList<Job> queue = new LinkedList<>();
    private final GPU[] gpus = new GPU[GPU_COUNT];
    private int nextJobNumber = 101;

    public Scheduler(SimulationClock clock, AccountService accounts) {
        if (clock == null || accounts == null) {
            throw new IllegalArgumentException("Clock and accounts are required.");
        }
        this.clock = clock;
        this.accounts = accounts;
        for (int i = 0; i < gpus.length; i++) {
            gpus[i] = new GPU(i + 1);
        }
    }

    public Job submit(Student student, String title, String applicationName, String description, TaskSize size) throws RuleException {
        if (!clock.isSet()) {
            throw new RuleException("Set the simulation clock before submitting jobs.");
        }
        if (student == null || accounts.findStudent(student.getUsername()) == null) {
            throw new RuleException("Only an approved student can submit a job.");
        }
        if (!Validator.notBlank(title) || !Validator.notBlank(applicationName) || !Validator.notBlank(description) || size == null) {
            throw new RuleException("Enter a title, application, description, and task size.");
        }

        Student owner = accounts.findStudent(student.getUsername());
        int duration = size.getUnits();
        owner.getAllowance().reserve(duration);

        String jobId = "J" + nextJobNumber;
        Job job = new Job(jobId, owner.getUsername(), title.trim(), applicationName.trim(), description.trim(), size, clock.now());
        allJobs.add(job);
        queue.addLast(job);
        nextJobNumber++;
        dispatch();
        return job;
    }

    public void advanceMinute() throws RuleException {
        clock.advance();
        for (GPU gpu : gpus) {
            Job running = gpu.getCurrentJob();
            if (running != null && running.minutesRun(clock.now()) >= running.getDuration()) {
                running.complete(clock.now());
                settle(running, running.getDuration());
                gpu.release();
            }
        }
        dispatch();
    }

    public void cancel(Job job, User by, String reason) throws RuleException {
        if (job == null || !allJobs.contains(job)) {
            throw new RuleException("That job does not exist in this system.");
        }
        if (by == null) {
            throw new RuleException("The user cancelling the job could not be identified.");
        }
        User actor = findActor(by.getUsername());
        if (actor == null || !actor.canCancel(job)) {
            throw new RuleException("You do not have permission to cancel this job.");
        }
        if (!job.isCancellable()) {
            throw new RuleException("Job " + job.getJobId() + " is " + job.getStatus()
                    + " and cannot be cancelled.");
        }
        if (!Validator.notBlank(reason)) {
            throw new RuleException("A reason is required to cancel a job.");
        }

        boolean wasRunning = job.getStatus() == JobStatus.RUNNING;
        GPU runningGpu = wasRunning ? gpuFor(job) : null;
        if (wasRunning && runningGpu == null) {
            throw new RuleException("The GPU assigned to job " + job.getJobId() + " was not found.");
        }
        int runtime = wasRunning ? job.minutesRun(clock.now()) : 0;
        job.cancel(actor.getUsername(), reason, clock.now());
        if (wasRunning) {
            settle(job, runtime);
            runningGpu.release();
            dispatch();
        } else {
            queue.remove(job);
            settle(job, 0);
        }
    }

    public void cancel(Job job, String username, String reason) throws RuleException {
        if (!Validator.notBlank(username)) {
            throw new RuleException("The user cancelling the job could not be identified.");
        }
        User actor = findActor(username.trim());
        cancel(job, actor, reason);
    }

    private User findActor(String username) {
        Administrator admin = accounts.getAdmin();
        if (admin.getUsername().equals(username)) {
            return admin;
        }
        return accounts.findStudent(username);
    }

    private GPU gpuFor(Job job) {
        int id = job.getAssignedGpuId();
        return id >= 1 && id <= gpus.length && gpus[id - 1].getCurrentJob() == job ? gpus[id - 1] : null;
    }

    private void dispatch() throws RuleException {
        while (!queue.isEmpty()) {
            GPU free = firstFreeGpu();
            if (free == null) return;
            Job job = queue.removeFirst();
            job.start(free.getId(), clock.now());
            free.assign(job);
        }
    }

    private GPU firstFreeGpu() {
        for (GPU gpu : gpus) {
            if (gpu.isFree()) return gpu;
        }
        return null;
    }

    private void settle(Job job, int minutesToCharge) throws RuleException {
        if (job.isSettled()) return;
        Student owner = accounts.findStudent(job.getStudentId());
        if (owner == null) {
            throw new RuleException("The student account for job " + job.getJobId() + " was not found.");
        }
        owner.getAllowance().release(job.getDuration());
        owner.getAllowance().charge(minutesToCharge);
        job.markSettled();
    }

    public List<Job> getAllJobs() {
        return new ArrayList<>(allJobs);
    }

    public List<Job> getJobsOf(String studentId) {
        List<Job> result = new ArrayList<>();
        for (Job job : allJobs) {
            if (job.getStudentId().equals(studentId)) result.add(job);
        }
        return result;
    }

    public Job findJob(String id) {
        if (id == null) return null;
        for (Job job : allJobs) {
            if (job.getJobId().equalsIgnoreCase(id.trim())) return job;
        }
        return null;
    }

    public List<Job> getQueue() {
        return new ArrayList<>(queue);
    }

    public GPU[] getGpus() {
        return Arrays.copyOf(gpus, gpus.length);
    }

    public int queuePosition(Job job) {
        int index = queue.indexOf(job);
        return index < 0 ? 0 : index + 1;
    }

    public Map<Job, LocalDateTime> predictStarts() {
        Map<Job, LocalDateTime> starts = new LinkedHashMap<>();
        if (!clock.isSet()) return starts;
        LocalDateTime[] freeAt = new LocalDateTime[gpus.length];
        for (int i = 0; i < gpus.length; i++) {
            Job running = gpus[i].getCurrentJob();
            freeAt[i] = (running == null) ? clock.now() : running.getStartTime().plusMinutes(running.getDuration());
        }
        for (Job job : queue) {
            int best = 0;
            for (int i = 1; i < freeAt.length; i++) {
                if (freeAt[i].isBefore(freeAt[best])) best = i;
            }
            starts.put(job, freeAt[best]);
            freeAt[best] = freeAt[best].plusMinutes(job.getDuration());
        }
        return starts;
    }

    public LocalDateTime predictedStart(Job job) {
        if (job == null) return null;
        if (job.getStatus() == JobStatus.RUNNING || job.getStatus() == JobStatus.COMPLETED || (job.getStatus() == JobStatus.CANCELLED && job.getStartTime() != null)) {
            return job.getStartTime();
        }
        return predictStarts().get(job);
    }

    public LocalDateTime predictedFinish(Job job) {
        if (job == null) return null;
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED) {
            return job.getFinishTime();
        }
        LocalDateTime start = predictedStart(job);
        return start == null ? null : start.plusMinutes(job.getDuration());
    }

    public long remainingWait(Job job) {
        LocalDateTime start = predictedStart(job);
        return (start == null || !clock.isSet()) ? 0 : Math.max(0, ChronoUnit.MINUTES.between(clock.now(), start));
    }

    public long queueWait(Job job) {
        if (job == null || !clock.isSet()) return 0;
        LocalDateTime end = job.getStartTime();
        if (end == null) end = job.getCancelTime();
        if (end == null) end = clock.now();
        return Math.max(0, ChronoUnit.MINUTES.between(job.getSubmissionTime(), end));
    }

    public int minutesCharged(Job job) {
        if (job == null || job.getStartTime() == null) return 0;
        if (job.isSettled()) return job.minutesRun(clock.now());
        return job.getStatus() == JobStatus.RUNNING ? job.minutesRun(clock.now()) : 0;
    }

    public int count(JobStatus status) {
        int count = 0;
        for (Job job : allJobs) {
            if (job.getStatus() == status) count++;
        }
        return count;
    }

    public int busyGpuCount() {
        int count = 0;
        for (GPU gpu : gpus) {
            if (!gpu.isFree()) count++;
        }
        return count;
    }

    public String utilizationText() {
        long elapsed = clock.elapsedMinutes();
        if (elapsed == 0) return "N/A";
        long busy = 0;
        for (Job job : allJobs) busy += job.minutesRun(clock.now());
        return String.format(java.util.Locale.ROOT, "%.1f%%", busy * 100.0 / (GPU_COUNT * elapsed));
    }

    public void restore(List<Job> savedJobs, int savedNextJobNumber) throws RuleException {
        if (savedJobs == null || savedNextJobNumber < 101) {
            throw new RuleException("Saved scheduler data is invalid.");
        }
        allJobs.clear();
        queue.clear();
        for (GPU gpu : gpus) gpu.release();
        allJobs.addAll(savedJobs);

        List<Job> queued = new ArrayList<>();
        for (Job job : allJobs) {
            if (job.getStatus() == JobStatus.QUEUED) queued.add(job);
            if (job.getStatus() == JobStatus.RUNNING) {
                int id = job.getAssignedGpuId();
                if (id < 1 || id > gpus.length || !gpus[id - 1].isFree()) {
                    throw new RuleException("Saved jobs contain an invalid or duplicate GPU assignment.");
                }
                gpus[id - 1].assign(job);
            }
        }
        queued.sort(Comparator.comparing(Job::getSubmissionTime).thenComparingInt(j -> numericJobNumber(j.getJobId())));
        queue.addAll(queued);
        nextJobNumber = savedNextJobNumber;
    }

    private static int numericJobNumber(String id) {
        try {
            return Integer.parseInt(id.substring(1));
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE;
        }
    }

    public int getNextJobNumber() {
        return nextJobNumber;
    }
}
