package com.bingo.missilewars;

import org.bukkit.Location;

public class MapDefinition {
    private final String name;
    private String worldName;
    private Location redSpawn;
    private Location blueSpawn;

    public MapDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Location getRedSpawn() {
        return redSpawn;
    }

    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
    }

    public Location getBlueSpawn() {
        return blueSpawn;
    }

    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
    }

    public boolean isReady() {
        return worldName != null && redSpawn != null && blueSpawn != null;
    }
}
