package model;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Lines {
    private Lines() { }

    static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static String enc(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String dec(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    static String time(LocalDateTime t) {
        return t == null ? "" : t.format(TIME);
    }

    static LocalDateTime parseTime(String s) {
        return s.isEmpty() ? null : LocalDateTime.parse(s, TIME);
    }

    static String join(String... parts) {
        return String.join("|", parts);
    }

    static String[] split(String line, int count) throws RuleException {
        String[] parts = line.split("\\|", -1);
        if (parts.length != count) {
            throw new RuleException(
                    "Bad saved line: expected " + count + " fields.");
        }
        return parts;
    }
}