package de.example.probingo.config;

import de.example.probingo.game.GameMode;

public final class ConfigData {

    private final GameMode defaultMode;
    private final int timeLimitMinutes;
    private final boolean allowDiagonals;
    private final Difficulty difficulty;
    private final boolean allowNether;
    private final boolean allowEnd;
    private final boolean allowTeamChat;
    private final TimerDirection timerDirection;
    private final WinSettings winSettings;

    public ConfigData(GameMode defaultMode,
                      int timeLimitMinutes,
                      boolean allowDiagonals,
                      Difficulty difficulty,
                      boolean allowNether,
                      boolean allowEnd,
                      boolean allowTeamChat,
                      TimerDirection timerDirection,
                      WinSettings winSettings) {
        this.defaultMode = defaultMode;
        this.timeLimitMinutes = timeLimitMinutes;
        this.allowDiagonals = allowDiagonals;
        this.difficulty = difficulty;
        this.allowNether = allowNether;
        this.allowEnd = allowEnd;
        this.allowTeamChat = allowTeamChat;
        this.timerDirection = timerDirection;
        this.winSettings = winSettings;
    }

    public GameMode defaultMode() {
        return defaultMode;
    }

    public int timeLimitMinutes() {
        return timeLimitMinutes;
    }

    public boolean allowDiagonals() {
        return allowDiagonals;
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    public boolean allowNether() {
        return allowNether;
    }

    public boolean allowEnd() {
        return allowEnd;
    }

    public boolean allowTeamChat() {
        return allowTeamChat;
    }

    public TimerDirection timerDirection() {
        return timerDirection;
    }

    public WinSettings winSettings() {
        return winSettings;
    }
}
