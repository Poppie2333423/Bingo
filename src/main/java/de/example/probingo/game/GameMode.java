package de.example.probingo.game;

public enum GameMode {
    TEAMS,
    FFA;

    public static GameMode fromString(String value, GameMode fallback) {
        if (value == null) {
            return fallback;
        }
        return switch (value.toLowerCase()) {
            case "teams", "team" -> TEAMS;
            case "ffa", "solo", "einzel" -> FFA;
            default -> fallback;
        };
    }
}
