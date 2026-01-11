package com.bingo.missilewars;

public enum TeamColor {
    RED,
    BLUE;

    public static TeamColor fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.startsWith("r")) {
            return RED;
        }
        if (normalized.startsWith("b")) {
            return BLUE;
        }
        return null;
    }
}
