package model;

import java.time.LocalDateTime;

public class TimeGrant implements Storable {
    private final String studentId;
    private final int minutes;
    private final String reason;
    private final String adminName;
    private final LocalDateTime time;

    public TimeGrant(String studentId, int minutes, String reason, String adminName, LocalDateTime time) {
        this.studentId = studentId;
        this.minutes = minutes;
        this.reason = reason;
        this.adminName = adminName;
        this.time = time;
    }

    public String getStudentId() { return studentId; }
    public int getMinutes() { return minutes; }
    public String getReason() { return reason; }
    public String getAdminName() { return adminName; }
    public LocalDateTime getTime() { return time; }

    @Override
    public String toLine() {
        throw new UnsupportedOperationException("TODO: agree the line format with Ali (DataStore)");
    }
}