package storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;
import model.AccessRequest;
import model.RuleException;
import model.Storable;
import model.Student;
import model.Job;
import model.TimeGrant;

public class DataStore {

    private final Path studentsFile = Path.of("data/students.txt");
    private final Path requestsFile = Path.of("data/requests.txt");
    private final Path grantsFile = Path.of("data/grants.txt");
    private final Path jobsFile = Path.of("data/jobs.txt");
    private final Path clockFile = Path.of("data/clock.txt");
    private final Path schedulerFile = Path.of("data/scheduler.txt");

    private final DateTimeFormatter timeFormat =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void saveAccounts(AccountService accounts)
            throws RuleException {

        saveRecords(studentsFile, accounts.getStudents());
        saveRecords(requestsFile, accounts.getRequests());
        saveRecords(grantsFile, accounts.getGrants());
    }

    private void saveRecords(Path file,
                             List<? extends Storable> records)
            throws RuleException {

        List<String> lines = new ArrayList<>();

        for (Storable record : records) {
            lines.add(record.toLine());
        }

        FileHelper.writeLines(file, lines);
    }

    public void loadAccounts(AccountService accounts)
            throws RuleException {

        List<Student> students = new ArrayList<>();
        List<AccessRequest> requests = new ArrayList<>();
        List<TimeGrant> grants = new ArrayList<>();

        List<String> lines = FileHelper.readLines(studentsFile);

        for (int i = 0; i < lines.size(); i++) {
            try {
                students.add(Student.fromLine(lines.get(i)));
            } catch (RuleException | RuntimeException e) {
                throw fileError(studentsFile, i + 1, e);
            }
        }

        lines = FileHelper.readLines(requestsFile);

        for (int i = 0; i < lines.size(); i++) {
            try {
                requests.add(AccessRequest.fromLine(lines.get(i)));
            } catch (RuleException | RuntimeException e) {
                throw fileError(requestsFile, i + 1, e);
            }
        }

        lines = FileHelper.readLines(grantsFile);

        for (int i = 0; i < lines.size(); i++) {
            try {
                grants.add(TimeGrant.fromLine(lines.get(i)));
            } catch (RuleException | RuntimeException e) {
                throw fileError(grantsFile, i + 1, e);
            }
        }

        accounts.restore(students, requests, grants);
    }

    private RuleException fileError(Path file, int line,
                                    Exception error) {

        return new RuleException(
                "Could not load " + file
                + " at line " + line
                + ": " + error.getMessage()
        );
    }

    public void saveAll(AccountService accounts,
                    Scheduler scheduler,
                    SimulationClock clock)
        throws RuleException {

    saveAccounts(accounts);

    saveRecords(jobsFile, scheduler.getAllJobs());

    List<String> clockLines = new ArrayList<>();

    if (clock.isSet()) {
        clockLines.add(clock.getStartTime().format(timeFormat));
        clockLines.add(clock.now().format(timeFormat));
    }

    FileHelper.writeLines(clockFile, clockLines);

    List<String> schedulerLines = new ArrayList<>();
    schedulerLines.add(
            String.valueOf(scheduler.getNextJobNumber())
    );

    FileHelper.writeLines(schedulerFile, schedulerLines);
    }
    
    public void loadAll(AccountService accounts,
                    Scheduler scheduler,
                    SimulationClock clock)
        throws RuleException {

    List<Job> jobs = new ArrayList<>();
    List<String> lines = FileHelper.readLines(jobsFile);

    int minimumNextNumber = 101;

    for (int i = 0; i < lines.size(); i++) {
        try {
            Job job = Job.fromLine(lines.get(i));

            String id = job.getJobId();

            if (!id.matches("J[0-9]+")) {
                throw new IllegalArgumentException("Invalid job ID.");
            }

            int number = Integer.parseInt(id.substring(1));

            if (number < 101 || number == Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Job number is outside the supported range."
                );
            }

            minimumNextNumber = Math.max(
                    minimumNextNumber, number + 1
            );

            jobs.add(job);

        } catch (RuleException | RuntimeException e) {
            throw fileError(jobsFile, i + 1, e);
        }
    }

    List<String> clockLines = FileHelper.readLines(clockFile);

    LocalDateTime start = null;
    LocalDateTime current = null;

    if (!clockLines.isEmpty()) {
        if (clockLines.size() != 2) {
            throw new RuleException(
                    "Could not load " + clockFile
                    + ": expected two lines: start time and current time."
            );
        }

        try {
            start = LocalDateTime.parse(
                    clockLines.get(0), timeFormat
            );
        } catch (RuntimeException e) {
            throw fileError(clockFile, 1, e);
        }

        try {
            current = LocalDateTime.parse(
                    clockLines.get(1), timeFormat
            );

            if (current.isBefore(start)) {
                throw new IllegalArgumentException(
                        "Current time cannot be before start time."
                );
            }
        } catch (RuntimeException e) {
            throw fileError(clockFile, 2, e);
        }
    }

    if (!jobs.isEmpty() && current == null) {
        throw new RuleException(
                "Saved jobs exist, but " + clockFile
                + " does not contain the simulation times."
        );
    }

    List<String> schedulerLines =
            FileHelper.readLines(schedulerFile);

    int nextNumber = minimumNextNumber;

    if (!schedulerLines.isEmpty()) {
        if (schedulerLines.size() != 1) {
            throw new RuleException(
                    "Could not load " + schedulerFile
                    + ": expected one line containing the next job number."
            );
        }

        try {
            nextNumber = Integer.parseInt(schedulerLines.get(0));

            if (nextNumber < minimumNextNumber) {
                throw new IllegalArgumentException(
                        "Next job number would reuse an existing ID."
                );
            }
        } catch (RuntimeException e) {
            throw fileError(schedulerFile, 1, e);
        }
    }

    AccountService loadedAccounts = new AccountService();
    loadAccounts(loadedAccounts);

    SimulationClock loadedClock = new SimulationClock();
    loadedClock.restore(start, current);

    Scheduler loadedScheduler =
            new Scheduler(loadedClock, loadedAccounts);

    try {
        loadedScheduler.restore(jobs, nextNumber);
    } catch (RuleException | RuntimeException e) {
        throw new RuleException(
                "Could not restore " + jobsFile
                + ": " + e.getMessage()
        );
    }

    accounts.restore(
            loadedAccounts.getStudents(),
            loadedAccounts.getRequests(),
            loadedAccounts.getGrants()
    );

    clock.restore(start, current);
    scheduler.restore(jobs, nextNumber);
}
}