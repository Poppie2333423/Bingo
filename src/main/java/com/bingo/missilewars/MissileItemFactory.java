package com.bingo.missilewars;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class MissileItemFactory {
    private static final String ROCKET_KEY = "rocket-type";

    private MissileItemFactory() {
    }

    public static ItemStack createEgg(MissileWarsPlugin plugin, RocketType type) {
        ItemStack item = new ItemStack(type.getEggMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.getDisplayName(), NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public static RocketType getRocketType(MissileWarsPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        String value = container.get(key(plugin), PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return RocketType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static Component countdownBar(int secondsLeft) {
        int safe = Math.max(secondsLeft, 0);
        return Component.text("Nächstes Ei in: ", NamedTextColor.YELLOW)
            .append(Component.text(safe, NamedTextColor.GREEN));
    }

    private static NamespacedKey key(MissileWarsPlugin plugin) {
        return new NamespacedKey(plugin, ROCKET_KEY);
    }
}
