package de.example.probingo.util;

import java.util.Locale;
import org.bukkit.Material;

public final class MaterialNameFormatter {

    private MaterialNameFormatter() {
    }

    public static String displayName(Material material) {
        String name = material.name().toLowerCase(Locale.GERMAN).replace('_', ' ');
        String[] parts = name.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        if (builder.length() == 0) {
            return material.name();
        }
        return builder.substring(0, builder.length() - 1);
    }
}
