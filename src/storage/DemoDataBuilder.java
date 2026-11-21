package storage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;
import model.AccessRequest;
import model.Job;
import model.JobStatus;
import model.RuleException;
import model.Student;
import model.TaskSize;
import model.TimeAllowance;

public class DemoDataBuilder {

    public static void main(String[] args) throws RuleException {
        AccountService accounts = new AccountService();
        SimulationClock clock = new SimulationClock();
        Scheduler scheduler = new Scheduler(clock, accounts);

        clock.set(LocalDateTime.of(2026, 11, 16, 10, 0));

        String[] names = {"Ali", "Sara", "Hamad", "Mariam", "Noura"};
        Student[] students = new Student[names.length];

        for (int i = 0; i < names.length; i++) {
            String id = String.valueOf(1001 + i);

            AccessRequest request = accounts.register(
                    id, names[i], "student" + id + "@example.com",
                    "050123450" + i, "demo123"
            );

            students[i] = accounts.approve(
                    request, AccountService.DEFAULT_MINUTES
            );
        }

        accounts.register(
                "1006", "Omar", "omar@example.com",
                "0501234505", "demo123"
        );
        accounts.register(
                "1007", "Lina", "lina@example.com",
                "0501234506", "demo123"
        );

        AccessRequest rejected = accounts.register(
                "1008", "Ahmed", "ahmed@example.com",
                "0501234507", "demo123"
        );
        accounts.reject(rejected);

        for (int i = 0; i < 4; i++) {
            scheduler.submit(
                    students[i], "Data preparation " + (i + 1), "Python",
                    "Prepare the research dataset.", TaskSize.SMALL
            );
        }

        advanceMinutes(scheduler, 5);

        Job[] training = new Job[4];

        for (int i = 0; i < training.length; i++) {
            training[i] = scheduler.submit(
                    students[i], "Model training " + (i + 1), "PyTorch",
                    "Train a demonstration model.", TaskSize.MEDIUM
            );
        }

        advanceMinutes(scheduler, 2);

        scheduler.cancel(
                training[0], students[0], "Selected the wrong dataset."
        );
        scheduler.cancel(
                training[1], accounts.getAdmin(), "Administrator stopped a duplicate run."
        );

        accounts.grantTime(
                students[0], 20, "Extra time for the research project.",
                accounts.getAdmin().getName(), clock.now()
        );
        accounts.grantTime(
                students[4], 5, "Additional time for a final experiment.",
                accounts.getAdmin().getName(), clock.now()
        );

        advanceMinutes(scheduler, 3);

        TaskSize[] sizes = {
            TaskSize.LARGE, TaskSize.VERY_LARGE,
            TaskSize.SMALL, TaskSize.MEDIUM,
            TaskSize.VERY_LARGE, TaskSize.MEDIUM,
            TaskSize.SMALL, TaskSize.LARGE,
            TaskSize.MEDIUM, TaskSize.LARGE,
            TaskSize.MEDIUM, TaskSize.SMALL
        };

        Job[] research = new Job[sizes.length];

        for (int i = 0; i < research.length; i++) {
            research[i] = scheduler.submit(
                    students[i % students.length],
                    "Research task " + (i + 1), "TensorFlow",
                    "Run a simulated research experiment.", sizes[i]
            );
        }

        scheduler.cancel(
                research[2], students[2], "No longer need this queued experiment."
        );
        scheduler.cancel(
                research[3], accounts.getAdmin(), "Administrator removed a duplicate request."
        );

        new DataStore().saveAll(accounts, scheduler, clock);

        System.out.println("Demo data saved to data/.");
        System.out.println("Administrator: admin / gpu2026");
        System.out.println("Students 1001 to 1005: password demo123");
        System.out.println("Pending: 1006 and 1007. Rejected: 1008.");
        System.out.println("Simulation time: " + clock.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));

        for (JobStatus status : JobStatus.values()) {
            System.out.println(status + ": " + scheduler.count(status));
        }

        System.out.println("Utilization: " + scheduler.utilizationText());

        for (Student student : students) {
            TimeAllowance allowance = student.getAllowance();
            System.out.printf(
                    "%s %s: allocated=%d used=%d reserved=%d available=%d%n",
                    student.getUsername(), student.getName(),
                    allowance.getAllocated(), allowance.getUsed(),
                    allowance.getReserved(), allowance.available()
            );
        }
    }

    private static void advanceMinutes(Scheduler scheduler, int minutes)
            throws RuleException {
        for (int i = 0; i < minutes; i++) {
            scheduler.advanceMinute();
        }
    }
}
