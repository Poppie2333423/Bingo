package com.example.missilewars;

import org.bukkit.ChatColor;

public enum TeamColor {
    RED(ChatColor.RED + "Rot"),
    BLUE(ChatColor.BLUE + "Blau");

    private final String displayName;

    TeamColor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static TeamColor fromString(String input) {
        if (input == null) {
            return null;
        }
        switch (input.toLowerCase()) {
            case "red", "rot" -> {
                return RED;
            }
            case "blue", "blau" -> {
                return BLUE;
            }
            default -> {
                return null;
            }
        }
    }
}
