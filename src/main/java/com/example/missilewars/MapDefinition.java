package com.example.missilewars;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

public class MapDefinition {
    private final String worldName;
    private Location redSpawn;
    private Location blueSpawn;
    private Location portalPos1;
    private Location portalPos2;
    private final List<String> rocketPool = new ArrayList<>();

    public MapDefinition(String worldName) {
        this.worldName = worldName;
    }

    public String worldName() {
        return worldName;
    }

    public Location redSpawn() {
        return redSpawn;
    }

    public void redSpawn(Location location) {
        this.redSpawn = location;
    }

    public Location blueSpawn() {
        return blueSpawn;
    }

    public void blueSpawn(Location location) {
        this.blueSpawn = location;
    }

    public Location portalPos1() {
        return portalPos1;
    }

    public void portalPos1(Location location) {
        this.portalPos1 = location;
    }

    public Location portalPos2() {
        return portalPos2;
    }

    public void portalPos2(Location location) {
        this.portalPos2 = location;
    }

    public List<String> rocketPool() {
        return rocketPool;
    }
}
