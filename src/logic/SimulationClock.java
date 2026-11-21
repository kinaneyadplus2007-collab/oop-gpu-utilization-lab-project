package logic;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import model.RuleException;

public class SimulationClock {
    private LocalDateTime startTime;
    private LocalDateTime now;

    public boolean isSet() {
        return now != null;
    }

    public void set(LocalDateTime time) throws RuleException {
        if (time == null) {
            throw new RuleException("Choose a starting date and time.");
        }
        if (isSet()) {
            throw new RuleException("The clock is already set.");
        }
        startTime = time;
        now = time;
    }

    public void advance() throws RuleException {
        if (!isSet()) {
            throw new RuleException("Set the clock first.");
        }
        now = now.plusMinutes(1);
    }

    public LocalDateTime now() {
        return now;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public long elapsedMinutes() {
        return isSet() ? ChronoUnit.MINUTES.between(startTime, now) : 0;
    }

    public void restore(LocalDateTime start, LocalDateTime current) {
        if ((start == null) != (current == null)) {
            throw new IllegalArgumentException(
                    "Start and current time must both be set or both be null.");
        }
        if (start != null && current.isBefore(start)) {
            throw new IllegalArgumentException("Current time cannot be before the start time.");
        }
        startTime = start;
        now = current;
    }
}
