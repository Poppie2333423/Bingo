package com.bingo.missilewars;

import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

public enum RocketType {
    SMALL("Rakete Klein", Material.CREEPER_SPAWN_EGG) {
        @Override
        public void launch(Player player, Location spawnLocation) {
            SmallFireball fireball = spawnLocation.getWorld().spawn(spawnLocation, SmallFireball.class);
            fireball.setShooter(player);
            fireball.setDirection(directionFor(player));
        }
    },
    MEDIUM("Rakete Mittel", Material.GHAST_SPAWN_EGG) {
        @Override
        public void launch(Player player, Location spawnLocation) {
            Fireball fireball = spawnLocation.getWorld().spawn(spawnLocation, Fireball.class);
            fireball.setShooter(player);
            fireball.setDirection(directionFor(player));
        }
    },
    LARGE("Rakete Groß", Material.WITHER_SPAWN_EGG) {
        @Override
        public void launch(Player player, Location spawnLocation) {
            WitherSkull skull = spawnLocation.getWorld().spawn(spawnLocation, WitherSkull.class);
            skull.setShooter(player);
            skull.setDirection(directionFor(player));
        }
    };

    private static final List<RocketType> VALUES = List.of(values());
    private final String displayName;
    private final Material eggMaterial;

    RocketType(String displayName, Material eggMaterial) {
        this.displayName = displayName;
        this.eggMaterial = eggMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getEggMaterial() {
        return eggMaterial;
    }

    public abstract void launch(Player player, Location spawnLocation);

    public static RocketType random(Random random) {
        return VALUES.get(random.nextInt(VALUES.size()));
    }

    protected Vector directionFor(Player player) {
        return player.getLocation().getDirection().normalize();
    }
}
