package test;

import java.time.LocalDateTime;
import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;
import model.*;
import storage.DataStore;

public class StorageTest {

    private static void check(String label, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAIL: " + label);
        }

        System.out.println("PASS: " + label);
    }

    public static void main(String[] args) throws RuleException {
        AccountService accounts = new AccountService();
        SimulationClock clock = new SimulationClock();
        Scheduler scheduler = new Scheduler(clock, accounts);
        DataStore store = new DataStore();

        clock.set(LocalDateTime.of(2026, 11, 16, 10, 0));

        AccessRequest approved = accounts.register(
                "1001", "Ali", "ali@example.com",
                "0501234567", "test123"
        );

        Student student = accounts.approve(approved, 100);

        accounts.register(
                "1002", "Sara", "sara@example.com",
                "0501234568", "test456"
        );

        AccessRequest rejected = accounts.register(
                "1003", "Ahmed", "ahmed@example.com",
                "0501234569", "test789"
        );

        accounts.reject(rejected);

        accounts.grantTime(
                student, 50, "Storage test",
                accounts.getAdmin().getName(), clock.now()
        );

        Job completed = scheduler.submit(
                student, "Small job", "Java",
                "Will complete", TaskSize.SMALL
        );

        Job cancelled = scheduler.submit(
                student, "Cancelled job", "Java",
                "Will cancel while running", TaskSize.MEDIUM
        );

        scheduler.submit(
                student, "Running A", "Java",
                "Keep running", TaskSize.LARGE
        );

        scheduler.submit(
                student, "Running B", "Java",
                "Keep running", TaskSize.LARGE
        );

        scheduler.submit(
                student, "Waiting A", "Java",
                "Will start after cancellation", TaskSize.MEDIUM
        );

        Job queuedCancel = scheduler.submit(
                student, "Queued cancellation", "Java",
                "Cancel without charging", TaskSize.SMALL
        );

        scheduler.submit(
                student, "Waiting B", "Java",
                "Will start after completion", TaskSize.LARGE
        );

        scheduler.submit(
                student, "Waiting C", "Java",
                "Remain queued", TaskSize.MEDIUM
        );

        scheduler.cancel(
                queuedCancel, student, "Test queued cancellation"
        );

        for (int i = 0; i < 2; i++) {
            scheduler.advanceMinute();
        }

        scheduler.cancel(
                cancelled, student, "Test running cancellation"
        );

        for (int i = 0; i < 3; i++) {
            scheduler.advanceMinute();
        }

        check("Completed job", completed.getStatus()
                == JobStatus.COMPLETED);

        check("Running cancellation charged 2 minutes",
                scheduler.minutesCharged(cancelled) == 2);

        check("Queued cancellation charged nothing",
                scheduler.minutesCharged(queuedCancel) == 0);

        store.saveAll(accounts, scheduler, clock);

        AccountService restoredAccounts = new AccountService();
        SimulationClock restoredClock = new SimulationClock();

        Scheduler restoredScheduler =
                new Scheduler(restoredClock, restoredAccounts);

        store.loadAll(
                restoredAccounts, restoredScheduler, restoredClock
        );

        check("Student login restored",
                restoredAccounts.login("1001", "test123")
                        instanceof Student);

        check("Registration requests restored",
                restoredAccounts.getRequests().size() == 3);

        check("Pending request restored",
                restoredAccounts.getRequests().get(1).getStatus()
                        == RequestStatus.PENDING);

        check("Rejected request restored",
                restoredAccounts.getRequests().get(2).getStatus()
                        == RequestStatus.REJECTED);

        check("Grant restored",
                restoredAccounts.getGrants().size() == 1
                && restoredAccounts.getGrants().get(0).toLine()
                        .equals(accounts.getGrants().get(0).toLine()));

        TimeAllowance original = student.getAllowance();
        TimeAllowance restored =
                restoredAccounts.findStudent("1001").getAllowance();

        check("Allocated balance restored",
                original.getAllocated() == restored.getAllocated());

        check("Used balance restored",
                original.getUsed() == restored.getUsed());

        check("Reserved balance restored",
                original.getReserved() == restored.getReserved());

        check("Available balance restored",
                original.available() == restored.available());

        check("Clock start restored",
                clock.getStartTime().equals(
                        restoredClock.getStartTime()));

        check("Current time restored",
                clock.now().equals(restoredClock.now()));

        check("Elapsed time restored",
                clock.elapsedMinutes() == restoredClock.elapsedMinutes());

        check("Job count restored",
                scheduler.getAllJobs().size()
                        == restoredScheduler.getAllJobs().size());

        for (Job job : scheduler.getAllJobs()) {
            Job copy = restoredScheduler.findJob(job.getJobId());

            check("Full job record " + job.getJobId(),
                    copy != null
                    && job.toLine().equals(copy.toLine()));
        }

        check("Queue size restored",
                scheduler.getQueue().size()
                        == restoredScheduler.getQueue().size());

        for (int i = 0; i < scheduler.getQueue().size(); i++) {
            check("Queue position " + (i + 1),
                    scheduler.getQueue().get(i).getJobId().equals(
                            restoredScheduler.getQueue()
                                    .get(i).getJobId()));
        }

        GPU[] originalGpus = scheduler.getGpus();
        GPU[] restoredGpus = restoredScheduler.getGpus();

        for (int i = 0; i < originalGpus.length; i++) {
            Job before = originalGpus[i].getCurrentJob();
            Job after = restoredGpus[i].getCurrentJob();

            check("GPU " + originalGpus[i].getId(),
                    before == null
                            ? after == null
                            : after != null
                            && before.getJobId().equals(after.getJobId()));
        }

        check("Next job number restored",
                scheduler.getNextJobNumber()
                        == restoredScheduler.getNextJobNumber());

        check("Utilization restored",
                scheduler.utilizationText().equals(
                        restoredScheduler.utilizationText()));

        restoredScheduler.advanceMinute();

        check("Clock continues from saved time",
                restoredClock.now().equals(clock.now().plusMinutes(1)));

        System.out.println("All storage checks passed.");
    }
}