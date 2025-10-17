package de.example.probingo.config;

public final class WinSettings {

    private final boolean blackout;
    private final int linesRequired;

    public WinSettings(boolean blackout, int linesRequired) {
        this.blackout = blackout;
        this.linesRequired = Math.max(1, linesRequired);
    }

    public boolean blackout() {
        return blackout;
    }

    public int linesRequired() {
        return linesRequired;
    }
}
