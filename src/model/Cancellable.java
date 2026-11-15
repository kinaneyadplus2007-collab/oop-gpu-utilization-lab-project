package model;

import java.time.LocalDateTime;

public interface Cancellable {
    boolean isCancellable();

    void cancel(String by, String reason, LocalDateTime now) throws RuleException;
}