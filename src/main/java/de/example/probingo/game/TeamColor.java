package de.example.probingo.game;

import net.kyori.adventure.text.format.NamedTextColor;

public enum TeamColor {
    RED("Rot", NamedTextColor.RED),
    BLUE("Blau", NamedTextColor.BLUE);

    private final String displayName;
    private final NamedTextColor color;

    TeamColor(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor color() {
        return color;
    }
}
