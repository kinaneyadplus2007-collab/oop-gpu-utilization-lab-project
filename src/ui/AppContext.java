package ui;

import java.util.*;
import logic.*;

public class AppContext {
    private final SimulationClock clock;
    private final AccountService accounts;
    private final Scheduler scheduler;
    private final List<Refreshable> panels = new ArrayList<>();
    private Runnable saver = () -> { };

    public AppContext(SimulationClock clock, AccountService accounts, Scheduler scheduler) {
        if (clock == null || accounts == null || scheduler == null) {
            throw new IllegalArgumentException("Clock, accounts, and scheduler are required.");
        }
        this.clock = clock;
        this.accounts = accounts;
        this.scheduler = scheduler;
    }

    public SimulationClock getClock() {
        return clock;
    }

    public AccountService getAccounts() {
        return accounts;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void register(Refreshable panel) {
        if (panel != null && !panels.contains(panel)) {
            panels.add(panel);
        }
    }

    public void unregister(Refreshable panel) {
        panels.remove(panel);
    }

    public void setSaver(Runnable saver) {
        this.saver = saver == null ? () -> { } : saver;
    }

    public void afterChange() {
        saver.run();
        for (Refreshable panel : new ArrayList<>(panels)) {
            panel.refresh();
        }
    }
}
