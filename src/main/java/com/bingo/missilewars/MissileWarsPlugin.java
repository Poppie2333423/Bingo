package com.bingo.missilewars;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MissileWarsPlugin extends JavaPlugin {
    private MapStore mapStore;
    private MissileWarsSession session;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mapStore = new MapStore(this);
        mapStore.load();

        StartCommand startCommand = new StartCommand(this);
        MissileWarsCommand missileWarsCommand = new MissileWarsCommand(this);

        PluginCommand start = getCommand("start");
        Objects.requireNonNull(start).setExecutor(startCommand);

        PluginCommand missileWars = getCommand("missilewars");
        Objects.requireNonNull(missileWars).setExecutor(missileWarsCommand);
        missileWars.setTabCompleter(missileWarsCommand);

        getServer().getPluginManager().registerEvents(new MissileWarsRocketListener(this), this);
    }

    @Override
    public void onDisable() {
        if (session != null) {
            session.stop();
        }
        if (mapStore != null) {
            mapStore.save();
        }
    }

    public MapStore getMapStore() {
        return mapStore;
    }

    public String getActiveMapName() {
        return getConfig().getString("active-map");
    }

    public void setActiveMapName(String name) {
        getConfig().set("active-map", name);
        saveConfig();
    }

    public void startSession(MapDefinition map, java.util.List<org.bukkit.entity.Player> players) {
        if (session != null) {
            session.stop();
        }
        session = new MissileWarsSession(this, map, players);
        session.start();
    }

    public MissileWarsSession getSession() {
        return session;
    }
}
