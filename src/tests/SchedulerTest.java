package tests;

import java.time.*;
import logic.*;
import model.*;

public final class SchedulerTest {
    private SchedulerTest() { }

    public static void main(String[] args) throws Exception {
        AccountService accounts = new AccountService();
        SimulationClock clock = new SimulationClock();
        Scheduler scheduler = new Scheduler(clock, accounts);
        Student[] students = new Student[5];

        for (int i = 0; i < students.length; i++) {
            students[i] = approved(accounts, "10000000" + i, "Student " + i);
        }

        clock.set(LocalDateTime.of(2026, 11, 16, 10, 0));
        Job j101 = scheduler.submit(students[0], "Job 101", "App", "Scenario", TaskSize.SMALL);
        Job j102 = scheduler.submit(students[1], "Job 102", "App", "Scenario", TaskSize.MEDIUM);
        Job j103 = scheduler.submit(students[2], "Job 103", "App", "Scenario", TaskSize.MEDIUM);
        Job j104 = scheduler.submit(students[3], "Job 104", "App", "Scenario", TaskSize.LARGE);

        advanceTo(clock, scheduler, 10, 8);
        Job j105 = scheduler.submit(students[4], "Job 105", "App", "Scenario", TaskSize.LARGE);
        advanceTo(clock, scheduler, 10, 10);
        Job j106 = scheduler.submit(students[0], "Job 106", "App", "Scenario", TaskSize.MEDIUM);
        advanceTo(clock, scheduler, 10, 12);
        Job j107 = scheduler.submit(students[1], "Job 107", "App", "Scenario", TaskSize.LARGE);
        advanceTo(clock, scheduler, 10, 16);
        Job j108 = scheduler.submit(students[2], "Job 108", "App", "Scenario", TaskSize.MEDIUM);
        advanceTo(clock, scheduler, 10, 18);
        Job j109 = scheduler.submit(students[3], "Job 109", "App", "Scenario", TaskSize.SMALL);
        advanceTo(clock, scheduler, 10, 19);
        Job j110 = scheduler.submit(students[4], "Job 110", "App", "Scenario", TaskSize.SMALL);
        scheduler.cancel(j110, accounts.getAdmin(), "Queued cancellation scenario");
        advanceTo(clock, scheduler, 10, 20);

        check("J101 complete/GPU1", j101.getStatus() == JobStatus.COMPLETED && j101.getAssignedGpuId() == 1);
        check("J102 complete/GPU2", j102.getStatus() == JobStatus.COMPLETED && j102.getAssignedGpuId() == 2);
        check("J103 complete/GPU3", j103.getStatus() == JobStatus.COMPLETED && j103.getAssignedGpuId() == 3);
        check("J104 running/GPU4", j104.getStatus() == JobStatus.RUNNING && j104.getAssignedGpuId() == 4);
        check("J105 running/GPU1", j105.getStatus() == JobStatus.RUNNING && j105.getAssignedGpuId() == 1);
        check("J106 running/GPU2", j106.getStatus() == JobStatus.RUNNING && j106.getAssignedGpuId() == 2);
        check("J107 running/GPU3", j107.getStatus() == JobStatus.RUNNING && j107.getAssignedGpuId() == 3);
        check("J108 queue position/start/wait", scheduler.queuePosition(j108) == 1 && scheduler.predictedStart(j108).equals(LocalDateTime.of(2026, 11, 16, 10, 30)) && scheduler.remainingWait(j108) == 10);
        check("J109 queue position/start/wait", scheduler.queuePosition(j109) == 2 && scheduler.predictedStart(j109).equals(LocalDateTime.of(2026, 11, 16, 10, 30)) && scheduler.remainingWait(j109) == 10);
        check("J110 cancelled without charge", j110.getStatus() == JobStatus.CANCELLED && scheduler.minutesCharged(j110) == 0);
        check("utilization at 10:20", "96.3%".equals(scheduler.utilizationText()));

        Job j111 = scheduler.submit(students[4], "Job 111", "App", "Scenario", TaskSize.LARGE);
        check("J111 predicted start/wait", scheduler.predictedStart(j111).equals(LocalDateTime.of(2026, 11, 16, 10, 35)) && scheduler.remainingWait(j111) == 15);

        scheduler.advanceMinute();
        check("waits after 10:21", scheduler.remainingWait(j108) == 9 && scheduler.remainingWait(j109) == 9 && scheduler.remainingWait(j111) == 14);

        System.out.println("All scheduler scenario checks passed.");
    }

    private static Student approved(AccountService accounts, String id, String name)
            throws RuleException {
        AccessRequest request = accounts.register(id, name, id + "@ku.ac.ae", "0501234567", "pw12345");
        accounts.approve(request, 100);
        return accounts.findStudent(id);
    }

    private static void advanceTo(SimulationClock clock, Scheduler scheduler, int hour, int minute) throws RuleException {
        LocalDateTime target = LocalDateTime.of(2026, 11, 16, hour, minute);
        while (clock.now().isBefore(target)) {
            scheduler.advanceMinute();
        }
    }

    private static void check(String label, boolean passed) {
        if (!passed) {
            throw new AssertionError(label + " failed");
        }
        System.out.println("PASS: " + label);
    }
}
