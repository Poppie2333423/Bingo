package com.example.missilewars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

public class MissileWarsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String ROCKET_KEY = "rocket-name";
    private static final String WAND_NAME = ChatColor.GOLD + "Missile Wars Wand";

    private final Map<String, MapDefinition> maps = new HashMap<>();
    private final Map<String, RocketDefinition> rockets = new HashMap<>();
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Map<UUID, TeamColor> teams = new HashMap<>();
    private final Map<String, GameSession> activeGames = new HashMap<>();
    private final Random random = new Random();

    private NamespacedKey rocketKey;

    @Override
    public void onEnable() {
        rocketKey = new NamespacedKey(this, ROCKET_KEY);
        saveDefaultConfig();
        loadState();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("missilewars")).setExecutor(this);
        Objects.requireNonNull(getCommand("missilewars")).setTabCompleter(this);
        Objects.requireNonNull(getCommand("start")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        saveState();
        activeGames.values().forEach(GameSession::stop);
        activeGames.clear();
    }

    @EventHandler
    public void onWandInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getType() != Material.WOODEN_AXE) {
            return;
        }
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null || !WAND_NAME.equals(meta.getDisplayName())) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Selection selection = selections.computeIfAbsent(event.getPlayer().getUniqueId(), key -> new Selection());
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Pos1 gesetzt.");
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.GREEN + "Pos2 gesetzt.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRocketUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        String rocketName = meta.getPersistentDataContainer().get(rocketKey, PersistentDataType.STRING);
        if (rocketName == null) {
            return;
        }
        RocketDefinition rocket = rockets.get(rocketName.toLowerCase(Locale.ROOT));
        if (rocket == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Diese Rakete existiert nicht mehr.");
            return;
        }
        TeamColor team = teams.get(event.getPlayer().getUniqueId());
        if (team == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Du bist keinem Team zugeordnet.");
            return;
        }
        Block target = event.getClickedBlock();
        if (target == null) {
            return;
        }
        BlockVector base = new BlockVector(target.getX(), target.getY() + 1, target.getZ());
        BlockFaceDirection spawnDirection = team == TeamColor.RED ? rocket.direction() : rocket.direction().opposite();
        placeRocket(target.getWorld(), base, rocket, spawnDirection);
        if (event.getPlayer().getGameMode().name().equalsIgnoreCase("SURVIVAL")) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void placeRocket(World world, BlockVector base, RocketDefinition rocket, BlockFaceDirection direction) {
        int rotations = rocket.direction().rotationsTo(direction);
        for (RocketBlock block : rocket.blocks()) {
            BlockVector rotated = rotate(block.offset(), rotations);
            Block target = world.getBlockAt(base.getBlockX() + rotated.getBlockX(), base.getBlockY() + rotated.getBlockY(), base.getBlockZ() + rotated.getBlockZ());
            BlockData data = Bukkit.createBlockData(block.blockData());
            BlockData rotatedData = rotateBlockData(data, rotations);
            target.setBlockData(rotatedData, false);
        }
        Material pistonMaterial = rocket.pistonMaterial();
        if (pistonMaterial != null) {
            for (RocketBlock block : rocket.blocks()) {
                if (block.material() == pistonMaterial) {
                    BlockVector rotated = rotate(block.offset(), rotations);
                    Block pistonBlock = world.getBlockAt(base.getBlockX() + rotated.getBlockX(), base.getBlockY() + rotated.getBlockY(), base.getBlockZ() + rotated.getBlockZ());
                    BlockData pistonData = pistonBlock.getBlockData();
                    if (pistonData instanceof Piston piston) {
                        piston.setExtended(true);
                        pistonBlock.setBlockData(piston, false);
                    }
                }
            }
        }
    }

    private BlockVector rotate(BlockVector vector, int rotations) {
        int x = vector.getBlockX();
        int z = vector.getBlockZ();
        int y = vector.getBlockY();
        int normalized = ((rotations % 4) + 4) % 4;
        return switch (normalized) {
            case 1 -> new BlockVector(-z, y, x);
            case 2 -> new BlockVector(-x, y, -z);
            case 3 -> new BlockVector(z, y, -x);
            default -> vector;
        };
    }

    private BlockData rotateBlockData(BlockData data, int rotations) {
        int normalized = ((rotations % 4) + 4) % 4;
        BlockData current = data;
        for (int i = 0; i < normalized; i++) {
            if (current instanceof Directional directional) {
                directional.setFacing(BlockFaceDirection.fromFace(directional.getFacing()).rotateClockwise().toFace());
            }
            if (current instanceof Rotatable rotatable) {
                rotatable.setRotation(BlockFaceDirection.fromFace(rotatable.getRotation()).rotateClockwise().toFace());
            }
            if (current instanceof Orientable orientable) {
                switch (orientable.getAxis()) {
                    case X -> orientable.setAxis(Orientable.Axis.Z);
                    case Z -> orientable.setAxis(Orientable.Axis.X);
                    default -> {
                    }
                }
            }
        }
        return current;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("start")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Nur Spieler können das Spiel starten.");
                return true;
            }
            MapDefinition map = getOrCreateMap(player.getWorld().getName());
            startGame(map, player.getWorld());
            sender.sendMessage(ChatColor.GREEN + "Missile Wars gestartet.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl nutzen.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "world" -> handleWorld(player, args);
            case "team" -> handleTeam(player, args);
            case "setspawn" -> handleSetSpawn(player, args);
            case "setportal" -> handleSetPortal(player);
            case "wand" -> handleWand(player);
            case "rocket" -> handleRocket(player, args);
            case "map" -> handleMap(player, args);
            case "info" -> handleInfo(player);
            case "start" -> {
                MapDefinition map = getOrCreateMap(player.getWorld().getName());
                startGame(map, player.getWorld());
                player.sendMessage(ChatColor.GREEN + "Missile Wars gestartet.");
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void handleWorld(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/missilewars world <weltname>");
            return;
        }
        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Welt nicht gefunden.");
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleportiert nach " + world.getName() + ".");
    }

    private void handleTeam(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/missilewars team <red|blue>");
            return;
        }
        TeamColor team = TeamColor.fromString(args[1]);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Unbekanntes Team.");
            return;
        }
        teams.put(player.getUniqueId(), team);
        player.sendMessage(ChatColor.GREEN + "Team gesetzt auf " + team.displayName() + ".");
    }

    private void handleSetSpawn(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/missilewars setspawn <red|blue>");
            return;
        }
        TeamColor team = TeamColor.fromString(args[1]);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Unbekanntes Team.");
            return;
        }
        MapDefinition map = getOrCreateMap(player.getWorld().getName());
        if (team == TeamColor.RED) {
            map.redSpawn(player.getLocation());
        } else {
            map.blueSpawn(player.getLocation());
        }
        saveState();
        player.sendMessage(ChatColor.GREEN + "Spawn gesetzt für " + team.displayName() + ".");
    }

    private void handleSetPortal(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage(ChatColor.RED + "Bitte zuerst Pos1 und Pos2 mit der Axt setzen.");
            return;
        }
        MapDefinition map = getOrCreateMap(player.getWorld().getName());
        map.portalPos1(selection.getPos1());
        map.portalPos2(selection.getPos2());
        saveState();
        player.sendMessage(ChatColor.GREEN + "Portalbereich gesetzt.");
    }

    private void handleWand(Player player) {
        ItemStack wand = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(WAND_NAME);
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "Wand erhalten.");
    }

    private void handleRocket(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/missilewars rocket <save|list|give>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "save" -> saveRocket(player, args);
            case "list" -> listRockets(player);
            case "give" -> giveRocket(player, args);
            default -> player.sendMessage(ChatColor.RED + "Unbekannter Unterbefehl.");
        }
    }

    private void saveRocket(Player player, String[] args) {
        if (args.length < 6) {
            player.sendMessage(ChatColor.RED + "/missilewars rocket save <name> <eggMaterial> <direction> <pistonMaterial>" );
            return;
        }
        String name = args[2];
        Material eggMaterial = Material.matchMaterial(args[3]);
        if (eggMaterial == null || !eggMaterial.name().endsWith("_SPAWN_EGG")) {
            player.sendMessage(ChatColor.RED + "Bitte ein gültiges Spawn-Ei Material angeben.");
            return;
        }
        BlockFaceDirection direction = BlockFaceDirection.fromString(args[4]);
        if (direction == null) {
            player.sendMessage(ChatColor.RED + "Richtung muss north/east/south/west sein.");
            return;
        }
        Material pistonMaterial = Material.matchMaterial(args[5]);
        if (pistonMaterial == null) {
            player.sendMessage(ChatColor.RED + "Ungültiges Piston-Material.");
            return;
        }
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage(ChatColor.RED + "Bitte zuerst Pos1 und Pos2 mit der Axt setzen.");
            return;
        }
        BlockVector min = selection.getMin();
        BlockVector max = selection.getMax();
        World world = player.getWorld();
        List<RocketBlock> blocks = new ArrayList<>();
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR || block.getType() == Material.VOID_AIR) {
                        continue;
                    }
                    BlockVector offset = new BlockVector(x - min.getBlockX(), y - min.getBlockY(), z - min.getBlockZ());
                    blocks.add(new RocketBlock(offset, block.getType(), block.getBlockData().getAsString()));
                }
            }
        }
        RocketDefinition definition = new RocketDefinition(name, eggMaterial, direction, pistonMaterial, blocks);
        rockets.put(name.toLowerCase(Locale.ROOT), definition);
        saveState();
        player.sendMessage(ChatColor.GREEN + "Rakete gespeichert: " + name + " (" + blocks.size() + " Blöcke)");
    }

    private void listRockets(Player player) {
        if (rockets.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Keine Raketen gespeichert.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Raketen: " + String.join(", ", rockets.keySet()));
    }

    private void giveRocket(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "/missilewars rocket give <name>");
            return;
        }
        RocketDefinition rocket = rockets.get(args[2].toLowerCase(Locale.ROOT));
        if (rocket == null) {
            player.sendMessage(ChatColor.RED + "Rakete nicht gefunden.");
            return;
        }
        player.getInventory().addItem(createRocketEgg(rocket));
        player.sendMessage(ChatColor.GREEN + "Raketen-Ei erhalten: " + rocket.name());
    }

    private void handleMap(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/missilewars map <addrocket>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("addrocket")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "/missilewars map addrocket <rocket>");
                return;
            }
            RocketDefinition rocket = rockets.get(args[2].toLowerCase(Locale.ROOT));
            if (rocket == null) {
                player.sendMessage(ChatColor.RED + "Rakete nicht gefunden.");
                return;
            }
            MapDefinition map = getOrCreateMap(player.getWorld().getName());
            map.rocketPool().add(rocket.name());
            saveState();
            player.sendMessage(ChatColor.GREEN + "Rakete zum Pool hinzugefügt.");
        } else {
            player.sendMessage(ChatColor.RED + "Unbekannter Unterbefehl.");
        }
    }

    private void handleInfo(Player player) {
        MapDefinition map = getOrCreateMap(player.getWorld().getName());
        player.sendMessage(ChatColor.AQUA + "--- Missile Wars Info ---");
        player.sendMessage(ChatColor.YELLOW + "Welt: " + map.worldName());
        player.sendMessage(ChatColor.YELLOW + "Red Spawn: " + (map.redSpawn() != null));
        player.sendMessage(ChatColor.YELLOW + "Blue Spawn: " + (map.blueSpawn() != null));
        player.sendMessage(ChatColor.YELLOW + "Portal gesetzt: " + (map.portalPos1() != null && map.portalPos2() != null));
        player.sendMessage(ChatColor.YELLOW + "Raketen Pool: " + String.join(", ", map.rocketPool()));
    }

    private void startGame(MapDefinition map, World world) {
        GameSession existing = activeGames.get(map.worldName().toLowerCase(Locale.ROOT));
        if (existing != null) {
            existing.stop();
        }
        GameSession session = new GameSession(map, world);
        activeGames.put(map.worldName().toLowerCase(Locale.ROOT), session);
        session.start();
    }

    private ItemStack createRocketEgg(RocketDefinition rocket) {
        ItemStack item = new ItemStack(rocket.eggMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + rocket.name());
        meta.getPersistentDataContainer().set(rocketKey, PersistentDataType.STRING, rocket.name().toLowerCase(Locale.ROOT));
        item.setItemMeta(meta);
        return item;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.AQUA + "Missile Wars Befehle:");
        player.sendMessage(ChatColor.YELLOW + "/missilewars world <welt>");
        player.sendMessage(ChatColor.YELLOW + "/missilewars team <red|blue>");
        player.sendMessage(ChatColor.YELLOW + "/missilewars setspawn <red|blue>");
        player.sendMessage(ChatColor.YELLOW + "/missilewars setportal");
        player.sendMessage(ChatColor.YELLOW + "/missilewars wand");
        player.sendMessage(ChatColor.YELLOW + "/missilewars rocket save <name> <egg> <direction> <piston>" );
        player.sendMessage(ChatColor.YELLOW + "/missilewars rocket give <name>" );
        player.sendMessage(ChatColor.YELLOW + "/missilewars map addrocket <rocket>" );
        player.sendMessage(ChatColor.YELLOW + "/missilewars info" );
        player.sendMessage(ChatColor.YELLOW + "/start" );
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("start")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("world", "team", "setspawn", "setportal", "wand", "rocket", "map", "info", "start"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("team")) {
            return filter(List.of("red", "blue"), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("setspawn")) {
            return filter(List.of("red", "blue"), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("world")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("rocket")) {
            if (args.length == 2) {
                return filter(List.of("save", "list", "give"), args[1]);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("save")) {
                return filter(Arrays.stream(Material.values()).map(Material::name).filter(name -> name.endsWith("_SPAWN_EGG")).collect(Collectors.toList()), args[3]);
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("save")) {
                return filter(List.of("north", "east", "south", "west"), args[4]);
            }
            if (args.length == 6 && args[1].equalsIgnoreCase("save")) {
                return filter(Arrays.stream(Material.values()).map(Material::name).collect(Collectors.toList()), args[5]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
                return filter(new ArrayList<>(rockets.keySet()), args[2]);
            }
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("map")) {
            if (args.length == 2) {
                return filter(List.of("addrocket"), args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("addrocket")) {
                return filter(new ArrayList<>(rockets.keySet()), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
            .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList());
    }

    private MapDefinition getOrCreateMap(String worldName) {
        return maps.computeIfAbsent(worldName.toLowerCase(Locale.ROOT), key -> new MapDefinition(worldName));
    }

    private void loadState() {
        maps.clear();
        rockets.clear();
        reloadConfig();
        ConfigurationSection rocketSection = getConfig().getConfigurationSection("rockets");
        if (rocketSection != null) {
            for (String key : rocketSection.getKeys(false)) {
                ConfigurationSection section = rocketSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String name = section.getString("name", key);
                Material egg = Material.matchMaterial(section.getString("egg", ""));
                BlockFaceDirection direction = BlockFaceDirection.fromString(section.getString("direction", "north"));
                Material piston = Material.matchMaterial(section.getString("piston", ""));
                List<Map<?, ?>> storedBlocks = section.getMapList("blocks");
                List<RocketBlock> blocks = new ArrayList<>();
                for (Map<?, ?> map : storedBlocks) {
                    int x = ((Number) map.get("x")).intValue();
                    int y = ((Number) map.get("y")).intValue();
                    int z = ((Number) map.get("z")).intValue();
                    String data = (String) map.get("data");
                    String materialName = (String) map.get("material");
                    Material material = Material.matchMaterial(materialName);
                    if (material == null) {
                        material = Material.AIR;
                    }
                    blocks.add(new RocketBlock(new BlockVector(x, y, z), material, data));
                }
                if (egg != null && direction != null) {
                    rockets.put(key.toLowerCase(Locale.ROOT), new RocketDefinition(name, egg, direction, piston, blocks));
                }
            }
        }
        ConfigurationSection mapSection = getConfig().getConfigurationSection("maps");
        if (mapSection != null) {
            for (String key : mapSection.getKeys(false)) {
                ConfigurationSection section = mapSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                String worldName = section.getString("world", key);
                MapDefinition map = new MapDefinition(worldName);
                map.redSpawn(readLocation(section.getConfigurationSection("red-spawn")));
                map.blueSpawn(readLocation(section.getConfigurationSection("blue-spawn")));
                map.portalPos1(readLocation(section.getConfigurationSection("portal-pos1")));
                map.portalPos2(readLocation(section.getConfigurationSection("portal-pos2")));
                map.rocketPool().addAll(section.getStringList("rocket-pool"));
                maps.put(key.toLowerCase(Locale.ROOT), map);
            }
        }
    }

    private void saveState() {
        getConfig().set("rockets", null);
        getConfig().set("maps", null);
        ConfigurationSection rocketSection = getConfig().createSection("rockets");
        for (RocketDefinition rocket : rockets.values()) {
            ConfigurationSection section = rocketSection.createSection(rocket.name().toLowerCase(Locale.ROOT));
            section.set("name", rocket.name());
            section.set("egg", rocket.eggMaterial().name());
            section.set("direction", rocket.direction().name().toLowerCase(Locale.ROOT));
            section.set("piston", rocket.pistonMaterial() != null ? rocket.pistonMaterial().name() : null);
            List<Map<String, Object>> blocks = new ArrayList<>();
            for (RocketBlock block : rocket.blocks()) {
                Map<String, Object> map = new HashMap<>();
                map.put("x", block.offset().getBlockX());
                map.put("y", block.offset().getBlockY());
                map.put("z", block.offset().getBlockZ());
                map.put("data", block.blockData());
                map.put("material", block.material().name());
                blocks.add(map);
            }
            section.set("blocks", blocks);
        }
        ConfigurationSection mapSection = getConfig().createSection("maps");
        for (MapDefinition map : maps.values()) {
            ConfigurationSection section = mapSection.createSection(map.worldName().toLowerCase(Locale.ROOT));
            section.set("world", map.worldName());
            writeLocation(section.createSection("red-spawn"), map.redSpawn());
            writeLocation(section.createSection("blue-spawn"), map.blueSpawn());
            writeLocation(section.createSection("portal-pos1"), map.portalPos1());
            writeLocation(section.createSection("portal-pos2"), map.portalPos2());
            section.set("rocket-pool", map.rocketPool());
        }
        saveConfig();
    }

    private void writeLocation(ConfigurationSection section, Location location) {
        if (location == null) {
            return;
        }
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        World world = Bukkit.getWorld(section.getString("world", ""));
        if (world == null) {
            return null;
        }
        return new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
    }

    private class GameSession {
        private final MapDefinition map;
        private final World world;
        private BukkitRunnable task;

        private GameSession(MapDefinition map, World world) {
            this.map = map;
            this.world = world;
        }

        private void start() {
            teleportTeams();
            giveStartItems();
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    giveTeamItems();
                }
            };
            task.runTaskTimer(MissileWarsPlugin.this, 0L, 200L);
        }

        private void stop() {
            if (task != null) {
                task.cancel();
            }
        }

        private void teleportTeams() {
            for (Player player : world.getPlayers()) {
                TeamColor team = teams.get(player.getUniqueId());
                if (team == null) {
                    continue;
                }
                Location spawn = team == TeamColor.RED ? map.redSpawn() : map.blueSpawn();
                if (spawn != null) {
                    player.teleport(spawn);
                }
            }
        }

        private void giveStartItems() {
            ItemStack bow = new ItemStack(Material.BOW);
            ItemMeta meta = bow.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.FLAME, 1, true);
            bow.setItemMeta(meta);
            for (Player player : world.getPlayers()) {
                TeamColor team = teams.get(player.getUniqueId());
                if (team == null) {
                    continue;
                }
                player.getInventory().addItem(bow.clone());
            }
        }

        private void giveTeamItems() {
            List<String> pool = map.rocketPool();
            for (Player player : world.getPlayers()) {
                TeamColor team = teams.get(player.getUniqueId());
                if (team == null) {
                    continue;
                }
                PlayerInventory inventory = player.getInventory();
                if (random.nextBoolean() || pool.isEmpty()) {
                    inventory.addItem(new ItemStack(Material.ARROW, 3));
                } else {
                    String rocketName = pool.get(random.nextInt(pool.size()));
                    RocketDefinition rocket = rockets.get(rocketName.toLowerCase(Locale.ROOT));
                    if (rocket != null) {
                        inventory.addItem(createRocketEgg(rocket));
                    }
                }
            }
        }
    }
}
