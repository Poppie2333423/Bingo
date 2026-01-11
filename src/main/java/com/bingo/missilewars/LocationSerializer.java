package com.bingo.missilewars;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class LocationSerializer {
    private LocationSerializer() {
    }

    public static Map<String, Object> toMap(Location location) {
        Map<String, Object> map = new HashMap<>();
        map.put("world", location.getWorld().getName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());
        map.put("yaw", location.getYaw());
        map.put("pitch", location.getPitch());
        return map;
    }

    public static Location fromMap(Map<?, ?> map) {
        Object worldName = map.get("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName.toString());
        if (world == null) {
            return null;
        }
        double x = getDouble(map, "x");
        double y = getDouble(map, "y");
        double z = getDouble(map, "z");
        float yaw = (float) getDouble(map, "yaw");
        float pitch = (float) getDouble(map, "pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private static double getDouble(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        return Double.parseDouble(value.toString());
    }
}
