package com.example.missilewars;

import org.bukkit.Location;
import org.bukkit.util.BlockVector;

public class Selection {
    private Location pos1;
    private Location pos2;

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public BlockVector getMin() {
        if (!isComplete()) {
            return null;
        }
        return new BlockVector(
            Math.min(pos1.getBlockX(), pos2.getBlockX()),
            Math.min(pos1.getBlockY(), pos2.getBlockY()),
            Math.min(pos1.getBlockZ(), pos2.getBlockZ())
        );
    }

    public BlockVector getMax() {
        if (!isComplete()) {
            return null;
        }
        return new BlockVector(
            Math.max(pos1.getBlockX(), pos2.getBlockX()),
            Math.max(pos1.getBlockY(), pos2.getBlockY()),
            Math.max(pos1.getBlockZ(), pos2.getBlockZ())
        );
    }
}
