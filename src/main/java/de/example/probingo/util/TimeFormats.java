package de.example.probingo.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFormats {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeFormats() {
    }

    public static String clock(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public static String timestamp(Instant instant) {
        if (instant == null) {
            return "--:--";
        }
        LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return CLOCK.format(time);
    }
}
