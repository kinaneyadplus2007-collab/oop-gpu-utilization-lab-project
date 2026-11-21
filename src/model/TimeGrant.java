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
        return Lines.join(
                Lines.enc(studentId),
                String.valueOf(minutes),
                Lines.enc(reason),
                Lines.enc(adminName),
                Lines.time(time)
        );
    }

    public static TimeGrant fromLine(String line) throws RuleException {
        String[] f = Lines.split(line, 5);

        try {
            return new TimeGrant(
                    Lines.dec(f[0]),
                    Integer.parseInt(f[1]),
                    Lines.dec(f[2]),
                    Lines.dec(f[3]),
                    Lines.parseTime(f[4])
            );
        } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
            throw new RuleException("Bad saved grant line.");
        }
    }
}