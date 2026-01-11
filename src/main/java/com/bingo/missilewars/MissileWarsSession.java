package com.bingo.missilewars;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class MissileWarsSession {
    private static final int GIVE_INTERVAL_SECONDS = 10;

    private final MissileWarsPlugin plugin;
    private final MapDefinition mapDefinition;
    private final List<UUID> players;
    private final Random random = new Random();
    private BukkitTask task;
    private int countdown = GIVE_INTERVAL_SECONDS;

    public MissileWarsSession(MissileWarsPlugin plugin, MapDefinition mapDefinition, List<Player> players) {
        this.plugin = plugin;
        this.mapDefinition = mapDefinition;
        this.players = new CopyOnWriteArrayList<>();
        for (Player player : players) {
            this.players.add(player.getUniqueId());
        }
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new ArrayList<>(players)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    players.remove(uuid);
                    continue;
                }
                player.sendActionBar(MissileItemFactory.countdownBar(countdown));
            }
            countdown--;
            if (countdown <= 0) {
                giveRandomEggs();
                countdown = GIVE_INTERVAL_SECONDS;
            }
        }, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public MapDefinition getMapDefinition() {
        return mapDefinition;
    }

    private void giveRandomEggs() {
        for (UUID uuid : new ArrayList<>(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                players.remove(uuid);
                continue;
            }
            RocketType type = RocketType.random(random);
            player.getInventory().addItem(MissileItemFactory.createEgg(plugin, type));
            player.sendMessage("§aDu hast ein §e" + type.getDisplayName() + "§a erhalten.");
        }
    }
}
