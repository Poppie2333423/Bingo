package com.bingo.missilewars;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class MissileWarsCommand implements CommandExecutor, TabCompleter {
    private final MissileWarsPlugin plugin;

    public MissileWarsCommand(MissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("map")) {
            return handleMap(sender, args);
        }
        sendHelp(sender);
        return true;
    }

    private boolean handleMap(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMapHelp(sender);
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "create" -> handleCreate(sender, args);
            case "setworld" -> handleSetWorld(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "select" -> handleSelect(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "teleport" -> handleTeleport(sender, args);
            default -> sendMapHelp(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cNutze: /missilewars map create <name>");
            return;
        }
        String name = args[2];
        MapDefinition definition = plugin.getMapStore().createMap(name);
        sender.sendMessage("§aMap erstellt/geladen: §e" + definition.getName());
    }

    private void handleSetWorld(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cNutze: /missilewars map setworld <name> <world>");
            return;
        }
        MapDefinition definition = getMap(sender, args[2]);
        if (definition == null) {
            return;
        }
        String worldName = args[3];
        if (Bukkit.getWorld(worldName) == null) {
            Bukkit.createWorld(new WorldCreator(worldName));
        }
        plugin.getMapStore().setWorld(definition, worldName);
        sender.sendMessage("§aWelt gesetzt: §e" + worldName);
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können Spawns setzen.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cNutze: /missilewars map setspawn <name> <red|blue>");
            return;
        }
        MapDefinition definition = getMap(sender, args[2]);
        if (definition == null) {
            return;
        }
        TeamColor team = TeamColor.fromString(args[3]);
        if (team == null) {
            sender.sendMessage("§cTeam muss red oder blue sein.");
            return;
        }
        Location location = player.getLocation();
        plugin.getMapStore().setSpawn(definition, team, location);
        sender.sendMessage("§aSpawn gesetzt für §e" + team.name().toLowerCase(Locale.ROOT));
    }

    private void handleSelect(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cNutze: /missilewars map select <name>");
            return;
        }
        MapDefinition definition = getMap(sender, args[2]);
        if (definition == null) {
            return;
        }
        plugin.setActiveMapName(definition.getName());
        sender.sendMessage("§aAktive Map gesetzt: §e" + definition.getName());
    }

    private void handleList(CommandSender sender) {
        List<MapDefinition> maps = plugin.getMapStore().listMaps();
        if (maps.isEmpty()) {
            sender.sendMessage("§7Keine Maps vorhanden.");
            return;
        }
        sender.sendMessage("§aMaps:");
        for (MapDefinition definition : maps) {
            sender.sendMessage("§7- §e" + definition.getName());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cNutze: /missilewars map info <name>");
            return;
        }
        MapDefinition definition = getMap(sender, args[2]);
        if (definition == null) {
            return;
        }
        sender.sendMessage("§aMap: §e" + definition.getName());
        sender.sendMessage("§7Welt: §f" + nullable(definition.getWorldName()));
        sender.sendMessage("§7Spawn Rot: §f" + (definition.getRedSpawn() != null));
        sender.sendMessage("§7Spawn Blau: §f" + (definition.getBlueSpawn() != null));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können sich teleportieren.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cNutze: /missilewars map teleport <name>");
            return;
        }
        MapDefinition definition = getMap(sender, args[2]);
        if (definition == null) {
            return;
        }
        if (definition.getWorldName() == null || definition.getWorldName().isBlank()) {
            sender.sendMessage("§cFür diese Map ist keine Welt gesetzt.");
            return;
        }
        if (Bukkit.getWorld(definition.getWorldName()) == null) {
            Bukkit.createWorld(new WorldCreator(definition.getWorldName()));
        }
        player.teleport(Bukkit.getWorld(definition.getWorldName()).getSpawnLocation());
        sender.sendMessage("§aTeleportiert zur Welt §e" + definition.getWorldName());
    }

    private MapDefinition getMap(CommandSender sender, String name) {
        MapDefinition definition = plugin.getMapStore().getMap(name);
        if (definition == null) {
            sender.sendMessage("§cMap nicht gefunden: " + name);
        }
        return definition;
    }

    private String nullable(String value) {
        if (value == null || value.isBlank()) {
            return "nicht gesetzt";
        }
        return value;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§eMissile Wars Setup:");
        sendMapHelp(sender);
    }

    private void sendMapHelp(CommandSender sender) {
        sender.sendMessage("§7/missilewars map create <name>");
        sender.sendMessage("§7/missilewars map setworld <name> <world>");
        sender.sendMessage("§7/missilewars map setspawn <name> <red|blue>");
        sender.sendMessage("§7/missilewars map select <name>");
        sender.sendMessage("§7/missilewars map teleport <name>");
        sender.sendMessage("§7/missilewars map list");
        sender.sendMessage("§7/missilewars map info <name>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("map").stream()
                .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }
        if (!args[0].equalsIgnoreCase("map")) {
            return List.of();
        }
        if (args.length == 2) {
            return List.of("create", "setworld", "setspawn", "select", "teleport", "list", "info").stream()
                .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }
        if (args.length == 3 && List.of("setworld", "setspawn", "select", "teleport", "info").contains(args[1].toLowerCase(Locale.ROOT))) {
            return plugin.getMapStore().listMaps().stream()
                .map(MapDefinition::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("setspawn")) {
            return List.of("red", "blue").stream()
                .filter(option -> option.startsWith(args[3].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
