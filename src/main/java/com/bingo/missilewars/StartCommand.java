package com.bingo.missilewars;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    private final MissileWarsPlugin plugin;

    public StartCommand(MissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String activeMap = plugin.getActiveMapName();
        if (activeMap == null || activeMap.isBlank()) {
            sender.sendMessage("§cKeine aktive Map gesetzt. Nutze /missilewars map select <name>.");
            return true;
        }
        MapDefinition definition = plugin.getMapStore().getMap(activeMap);
        if (definition == null) {
            sender.sendMessage("§cAktive Map nicht gefunden: " + activeMap);
            return true;
        }
        if (!definition.isReady()) {
            sender.sendMessage("§cMap ist nicht vollständig eingerichtet. Welt und beide Spawns setzen.");
            return true;
        }
        World world = Bukkit.getWorld(definition.getWorldName());
        if (world == null) {
            sender.sendMessage("§cWelt ist nicht geladen: " + definition.getWorldName());
            return true;
        }
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            sender.sendMessage("§cKeine Spieler online.");
            return true;
        }
        Location redSpawn = definition.getRedSpawn();
        Location blueSpawn = definition.getBlueSpawn();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i % 2 == 0) {
                player.teleport(redSpawn);
                player.sendMessage("§cDu bist im roten Team!");
            } else {
                player.teleport(blueSpawn);
                player.sendMessage("§9Du bist im blauen Team!");
            }
        }
        Bukkit.broadcastMessage("§aMissile Wars wurde gestartet auf Map §e" + definition.getName() + "§a.");
        return true;
    }
}
