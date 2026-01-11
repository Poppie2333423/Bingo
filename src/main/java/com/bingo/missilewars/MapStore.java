package com.bingo.missilewars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class MapStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, MapDefinition> maps = new ConcurrentHashMap<>();

    public MapStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "maps.yml");
    }

    public void load() {
        maps.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection mapsSection = config.getConfigurationSection("maps");
        if (mapsSection == null) {
            return;
        }
        for (String key : mapsSection.getKeys(false)) {
            ConfigurationSection mapSection = mapsSection.getConfigurationSection(key);
            if (mapSection == null) {
                continue;
            }
            MapDefinition definition = new MapDefinition(key);
            definition.setWorldName(mapSection.getString("world"));
            definition.setRedSpawn(loadLocation(mapSection, "spawns.red"));
            definition.setBlueSpawn(loadLocation(mapSection, "spawns.blue"));
            maps.put(key.toLowerCase(Locale.ROOT), definition);
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection mapsSection = config.createSection("maps");
        for (MapDefinition definition : maps.values()) {
            ConfigurationSection mapSection = mapsSection.createSection(definition.getName());
            if (definition.getWorldName() != null) {
                mapSection.set("world", definition.getWorldName());
            }
            saveLocation(mapSection, "spawns.red", definition.getRedSpawn());
            saveLocation(mapSection, "spawns.blue", definition.getBlueSpawn());
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Konnte maps.yml nicht speichern: " + exception.getMessage());
        }
    }

    public MapDefinition createMap(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        MapDefinition existing = maps.get(key);
        if (existing != null) {
            return existing;
        }
        MapDefinition definition = new MapDefinition(name);
        maps.put(key, definition);
        save();
        return definition;
    }

    public MapDefinition getMap(String name) {
        if (name == null) {
            return null;
        }
        return maps.get(name.toLowerCase(Locale.ROOT));
    }

    public List<MapDefinition> listMaps() {
        List<MapDefinition> list = new ArrayList<>(maps.values());
        list.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        return Collections.unmodifiableList(list);
    }

    public void setWorld(MapDefinition definition, String worldName) {
        definition.setWorldName(worldName);
        save();
    }

    public void setSpawn(MapDefinition definition, TeamColor team, Location location) {
        Objects.requireNonNull(team, "team");
        if (team == TeamColor.RED) {
            definition.setRedSpawn(location);
        } else {
            definition.setBlueSpawn(location);
        }
        save();
    }

    private Location loadLocation(ConfigurationSection section, String path) {
        ConfigurationSection locationSection = section.getConfigurationSection(path);
        if (locationSection == null) {
            return null;
        }
        return LocationSerializer.fromMap(locationSection.getValues(false));
    }

    private void saveLocation(ConfigurationSection section, String path, Location location) {
        if (location == null) {
            return;
        }
        section.createSection(path, LocationSerializer.toMap(location));
    }
}
