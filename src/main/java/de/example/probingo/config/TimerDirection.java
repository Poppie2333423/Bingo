package de.example.probingo.config;

public enum TimerDirection {
    UP,
    DOWN;

    public static TimerDirection fromString(String value, TimerDirection fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return TimerDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
