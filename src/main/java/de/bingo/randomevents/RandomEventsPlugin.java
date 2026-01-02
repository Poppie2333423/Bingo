package de.bingo.randomevents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Turtle;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class RandomEventsPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private static final int EVENT_INTERVAL_SECONDS = 30;

    private final Random random = new Random();
    private final List<EventDefinition> events = new ArrayList<>();
    private BossBar bossBar;
    private BukkitTask countdownTask;
    private int secondsRemaining = EVENT_INTERVAL_SECONDS;

    @Override
    public void onEnable() {
        getCommand("start").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        registerEvents();
    }

    @Override
    public void onDisable() {
        stopCountdown();
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("start")) {
            return false;
        }

        startCountdown();
        sender.sendMessage(ChatColor.GREEN + "RandomEvents gestartet! Alle 30 Sekunden passiert jetzt etwas.");
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (bossBar != null) {
            bossBar.removePlayer(event.getPlayer());
        }
    }

    private void startCountdown() {
        stopCountdown();
        secondsRemaining = EVENT_INTERVAL_SECONDS;
        bossBar = Bukkit.createBossBar("Nächstes Ereignis in 30s", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                secondsRemaining--;
                if (secondsRemaining <= 0) {
                    triggerRandomEvent();
                    secondsRemaining = EVENT_INTERVAL_SECONDS;
                }
                updateBossBar();
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void stopCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void updateBossBar() {
        if (bossBar == null) {
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, secondsRemaining / (double) EVENT_INTERVAL_SECONDS));
        bossBar.setProgress(progress);
        if (secondsRemaining <= 10) {
            bossBar.setColor(BarColor.RED);
        } else if (secondsRemaining <= 20) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
        bossBar.setTitle("Nächstes Ereignis in " + secondsRemaining + "s");
    }

    private void triggerRandomEvent() {
        if (events.isEmpty()) {
            return;
        }
        EventDefinition definition = events.get(random.nextInt(events.size()));
        Bukkit.broadcastMessage(ChatColor.GOLD + "⚡ Ereignis: " + ChatColor.YELLOW + definition.name());
        World world = getPrimaryWorld();
        definition.action().accept(world);
    }

    private World getPrimaryWorld() {
        return Bukkit.getWorlds().getFirst();
    }

    private Optional<Player> getRandomPlayer() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(players.get(random.nextInt(players.size())));
    }

    private void registerEvents() {
        events.clear();
        addEvent("Blitzschlag", world -> getRandomPlayer().ifPresent(player -> world.strikeLightning(player.getLocation())));
        addEvent("Geschwindigkeitsrausch", world -> applyPotionToAll(PotionEffectType.SPEED, 20 * 15, 1));
        addEvent("Sprungkraft", world -> applyPotionToAll(PotionEffectType.JUMP_BOOST, 20 * 15, 1));
        addEvent("Stärke", world -> applyPotionToAll(PotionEffectType.STRENGTH, 20 * 12, 0));
        addEvent("Regeneration", world -> applyPotionToAll(PotionEffectType.REGENERATION, 20 * 8, 1));
        addEvent("Zufälliger Trank", world -> giveRandomPotion());
        addEvent("Feuerwerkshow", world -> launchFireworks());
        addEvent("Hühnerregen", world -> spawnMobRain(EntityType.CHICKEN, 6));
        addEvent("Creeper in der Nähe", world -> spawnMobNearPlayers(EntityType.CREEPER, 1, 4));
        addEvent("Skelettpferd", world -> spawnMobNearPlayers(EntityType.SKELETON_HORSE, 1, 3));
        addEvent("Tag wird", world -> world.setTime(1000));
        addEvent("Nacht wird", world -> world.setTime(13000));
        addEvent("Regen startet", world -> world.setStorm(true));
        addEvent("Regen endet", world -> world.setStorm(false));
        addEvent("Diamantenregen", world -> dropItems(Material.DIAMOND, 4));
        addEvent("Goldregen", world -> dropItems(Material.GOLD_INGOT, 6));
        addEvent("TNT Lieferung", world -> dropItems(Material.TNT, 3));
        addEvent("Mini-Explosion", world -> getRandomPlayer().ifPresent(player -> world.createExplosion(player.getLocation(), 2.0F, false, false)));
        addEvent("Katapult", world -> Bukkit.getOnlinePlayers().forEach(player -> player.setVelocity(new Vector(0, 1.4, 0))));
        addEvent("Spieler tauschen Plätze", world -> swapPlayers());
        addEvent("Glow-Effekt", world -> applyPotionToAll(PotionEffectType.GLOWING, 20 * 12, 0));
        addEvent("Eisengolem-Wächter", world -> spawnMobNearPlayers(EntityType.IRON_GOLEM, 1, 3));
        addEvent("Bienen-Schwarm", world -> spawnMobNearPlayers(EntityType.BEE, 3, 3));
        addEvent("Wolf-Rudel", world -> spawnMobNearPlayers(EntityType.WOLF, 2, 3));
        addEvent("Zombiehorde", world -> spawnMobNearPlayers(EntityType.ZOMBIE, 4, 4));
        addEvent("Unsichtbarkeit", world -> applyPotionToAll(PotionEffectType.INVISIBILITY, 20 * 10, 0));
        addEvent("Slow Falling", world -> applyPotionToAll(PotionEffectType.SLOW_FALLING, 20 * 20, 0));
        addEvent("Schnellabbau", world -> applyPotionToAll(PotionEffectType.HASTE, 20 * 15, 1));
        addEvent("Sättigung", world -> applyPotionToAll(PotionEffectType.SATURATION, 20 * 6, 0));
        addEvent("Feuerresistenz", world -> applyPotionToAll(PotionEffectType.FIRE_RESISTANCE, 20 * 20, 0));
        addEvent("Schneeballregen", world -> dropItems(Material.SNOWBALL, 12));
        addEvent("Enderperlen-Geschenk", world -> dropItems(Material.ENDER_PEARL, 3));
        addEvent("Totem-Geschenk", world -> dropItems(Material.TOTEM_OF_UNDYING, 1));
        addEvent("Zufälliges Haustier", world -> spawnPet());
        addEvent("Schatzkiste", world -> dropTreasure());
        addEvent("Mini-Teleport", world -> teleportPlayersRandomly(6));
        addEvent("Sandsturm", world -> dropItems(Material.SAND, 10));
        addEvent("Slime-Party", world -> spawnMobNearPlayers(EntityType.SLIME, 2, 4));
        addEvent("Fledermaus-Schwarm", world -> spawnMobNearPlayers(EntityType.BAT, 5, 3));
        addEvent("Lama-Karawane", world -> spawnMobNearPlayers(EntityType.LLAMA, 2, 4));
        addEvent("Delfin-Schwarm", world -> spawnDolphins());
        addEvent("Fuchs-Rudel", world -> spawnMobNearPlayers(EntityType.FOX, 2, 4));
        addEvent("Schildkröten", world -> spawnMobNearPlayers(EntityType.TURTLE, 2, 4));
        addEvent("Pilzkuh", world -> spawnMobNearPlayers(EntityType.MOOSHROOM, 1, 4));
        addEvent("Schneegolems", world -> spawnMobNearPlayers(EntityType.SNOW_GOLEM, 2, 4));
        addEvent("Endermänner", world -> spawnMobNearPlayers(EntityType.ENDERMAN, 2, 4));
        addEvent("Hexe erscheint", world -> spawnMobNearPlayers(EntityType.WITCH, 1, 4));
        addEvent("Sturmböen", world -> applyPotionToAll(PotionEffectType.LEVITATION, 20 * 3, 0));
        addEvent("Glücksboost", world -> applyPotionToAll(PotionEffectType.LUCK, 20 * 20, 0));
        addEvent("Rüstungsglanz", world -> giveEnchantedArmor());
    }

    private void addEvent(String name, Consumer<World> action) {
        events.add(new EventDefinition(name, action));
    }

    private void applyPotionToAll(PotionEffectType type, int duration, int amplifier) {
        PotionEffect effect = new PotionEffect(type, duration, amplifier, true, true, true);
        Bukkit.getOnlinePlayers().forEach(player -> player.addPotionEffect(effect));
    }

    private void giveRandomPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        List<PotionEffectType> effects = List.of(
            PotionEffectType.SPEED,
            PotionEffectType.JUMP_BOOST,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.FIRE_RESISTANCE
        );
        PotionEffectType chosen = effects.get(random.nextInt(effects.size()));
        meta.addCustomEffect(new PotionEffect(chosen, 20 * 30, 0), true);
        meta.setColor(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
        meta.setDisplayName(ChatColor.AQUA + "Zufallstrank");
        potion.setItemMeta(meta);
        dropItemToAll(potion);
    }

    private void launchFireworks() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(1);
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)))
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
            firework.setFireworkMeta(meta);
        });
    }

    private void spawnMobRain(EntityType type, int amount) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location base = player.getLocation();
            for (int i = 0; i < amount; i++) {
                Location spawn = base.clone().add(randomOffset(3), 8 + random.nextInt(4), randomOffset(3));
                player.getWorld().spawnEntity(spawn, type);
            }
        });
    }

    private void spawnMobNearPlayers(EntityType type, int amount, int radius) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            for (int i = 0; i < amount; i++) {
                Location spawn = player.getLocation().clone().add(randomOffset(radius), 0, randomOffset(radius));
                spawn.setY(spawn.getWorld().getHighestBlockYAt(spawn) + 1);
                player.getWorld().spawnEntity(spawn, type);
            }
        });
    }

    private void swapPlayers() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) {
            return;
        }
        Collections.shuffle(players, random);
        Location first = players.get(0).getLocation();
        Location second = players.get(1).getLocation();
        players.get(0).teleport(second);
        players.get(1).teleport(first);
    }

    private void dropItems(Material material, int amountPerPlayer) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            ItemStack stack = new ItemStack(material, amountPerPlayer);
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        });
    }

    private void dropItemToAll(ItemStack item) {
        Bukkit.getOnlinePlayers().forEach(player -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void spawnPet() {
        getRandomPlayer().ifPresent(player -> {
            if (random.nextBoolean()) {
                Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation(), EntityType.WOLF);
                wolf.setTamed(true);
                wolf.setOwner(player);
            } else {
                Cat cat = (Cat) player.getWorld().spawnEntity(player.getLocation(), EntityType.CAT);
                cat.setTamed(true);
                cat.setOwner(player);
            }
        });
    }

    private void dropTreasure() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Schatzkiste");
        chest.setItemMeta(meta);
        ItemStack emeralds = new ItemStack(Material.EMERALD, 4);
        dropItemToAll(chest);
        dropItemToAll(emeralds);
    }

    private void teleportPlayersRandomly(int radius) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location base = player.getLocation();
            Location target = base.clone().add(randomOffset(radius), 0, randomOffset(radius));
            target.setY(target.getWorld().getHighestBlockYAt(target) + 1);
            player.teleport(target);
        });
    }

    private void spawnDolphins() {
        getRandomPlayer().ifPresent(player -> {
            Location location = player.getLocation();
            if (location.getBlock().isLiquid()) {
                player.getWorld().spawnEntity(location, EntityType.DOLPHIN);
                player.getWorld().spawnEntity(location, EntityType.DOLPHIN);
            }
        });
    }

    private void giveEnchantedArmor() {
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        helmet.addUnsafeEnchantment(Enchantment.PROTECTION, 3);
        helmet.addUnsafeEnchantment(Enchantment.UNBREAKING, 2);
        dropItemToAll(helmet);
    }

    private double randomOffset(int radius) {
        return random.nextInt(radius * 2 + 1) - radius;
    }

    private record EventDefinition(String name, Consumer<World> action) {
    }
}
