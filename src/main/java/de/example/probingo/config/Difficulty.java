package de.example.probingo.config;

public enum Difficulty {
    EASY,
    NORMAL,
    HARD;

    public static Difficulty fromString(String value, Difficulty fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Difficulty.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
