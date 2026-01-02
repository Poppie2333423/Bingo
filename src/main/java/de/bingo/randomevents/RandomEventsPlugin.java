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
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
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
        addEvent("Kettenblitze", world -> getRandomPlayer().ifPresent(player -> lightningChain(player.getLocation(), 4)));
        addEvent("Wither-Fluch", world -> applyPotionToAll(PotionEffectType.WITHER, 20 * 8, 1));
        addEvent("Blindes Chaos", world -> applyPotionToAll(PotionEffectType.BLINDNESS, 20 * 10, 0));
        addEvent("Nebel im Kopf", world -> applyPotionToAll(PotionEffectType.NAUSEA, 20 * 12, 0));
        addEvent("Eiseskälte", world -> applyPotionToAll(PotionEffectType.SLOWNESS, 20 * 15, 1));
        addEvent("Schwächeanfall", world -> applyPotionToAll(PotionEffectType.WEAKNESS, 20 * 15, 0));
        addEvent("Dunkelheit", world -> applyPotionToAll(PotionEffectType.DARKNESS, 20 * 8, 0));
        addEvent("Erschöpfung", world -> drainHunger());
        addEvent("Feuerzeichen", world -> setPlayersOnFire(5));
        addEvent("Magnet-Schub", world -> pullPlayersTogether(8));
        addEvent("Stoßwelle", world -> blastPlayersAway());
        addEvent("Knall und Rauch", world -> spawnParticleCloud(Particle.CAMPFIRE_COSY_SMOKE, 80));
        addEvent("Phantomfluch", world -> spawnMobNearPlayers(EntityType.PHANTOM, 2, 6));
        addEvent("Wächter aus der Tiefe", world -> spawnMobNearPlayers(EntityType.GUARDIAN, 1, 4));
        addEvent("Ravager-Alarm", world -> spawnMobNearPlayers(EntityType.RAVAGER, 1, 6));
        addEvent("Evoker-Zirkel", world -> spawnMobNearPlayers(EntityType.EVOKER, 1, 6));
        addEvent("Vex-Schwarm", world -> spawnMobNearPlayers(EntityType.VEX, 3, 5));
        addEvent("Magma-Sturz", world -> spawnMobRain(EntityType.MAGMA_CUBE, 4));
        addEvent("Skelett-Bogenschützen", world -> spawnMobNearPlayers(EntityType.SKELETON, 3, 5));
        addEvent("Creeper-Angriff", world -> spawnMobNearPlayers(EntityType.CREEPER, 2, 5));
        addEvent("Hoglin-Ansturm", world -> spawnMobNearPlayers(EntityType.HOGLIN, 2, 6));
        addEvent("Spinnenweben", world -> spawnMobNearPlayers(EntityType.CAVE_SPIDER, 3, 5));
        addEvent("Ender-Überraschung", world -> spawnMobNearPlayers(EntityType.ENDERMAN, 3, 6));
        addEvent("Schleimschlag", world -> spawnMobNearPlayers(EntityType.SLIME, 3, 5));
        addEvent("Donnersturm", world -> startThunderstorm());
        addEvent("Ascheregen", world -> spawnFallingBlocks(Material.GRAY_CONCRETE_POWDER, 6));
        addEvent("Anvil-Regen", world -> spawnFallingBlocks(Material.ANVIL, 3));
        addEvent("Lava-Spritzer", world -> spawnLavaBurst());
        addEvent("Stachelfeld", world -> placeThornsAroundPlayers());
        addEvent("Netherglut", world -> applyPotionToAll(PotionEffectType.FIRE_RESISTANCE, 20 * 5, 0));
        addEvent("Verwirrungsteleport", world -> teleportPlayersRandomly(12));
        addEvent("Zeitfrost", world -> applyPotionToAll(PotionEffectType.SLOW_FALLING, 20 * 8, 0));
        addEvent("Magische Erschütterung", world -> playSoundAll(Sound.ENTITY_WARDEN_SONIC_BOOM));
        addEvent("Tränengas", world -> applyPotionToAll(PotionEffectType.POISON, 20 * 8, 0));
        addEvent("Geringe Heilung", world -> applyPotionToAll(PotionEffectType.REGENERATION, 20 * 5, 0));
        addEvent("Lähmung", world -> applyPotionToAll(PotionEffectType.MINING_FATIGUE, 20 * 20, 1));
        addEvent("Glühende Ziele", world -> applyPotionToAll(PotionEffectType.GLOWING, 20 * 20, 0));
        addEvent("Schicksalswürfel", world -> giveRandomPotion());
        addEvent("Erdspalt", world -> createSmallExplosions());
        addEvent("TNT-Fracht", world -> dropItems(Material.TNT, 2));
        addEvent("Sandsack", world -> dropItems(Material.SAND, 16));
        addEvent("Schattenkatzen", world -> spawnPet());
        addEvent("Eisige Winde", world -> applyPotionToAll(PotionEffectType.SLOWNESS, 20 * 10, 2));
        addEvent("Schockwelle", world -> applyPotionToAll(PotionEffectType.LEVITATION, 20 * 2, 1));
        addEvent("Gruselmelodie", world -> playSoundAll(Sound.AMBIENT_CAVE));
        addEvent("Warnsirene", world -> playSoundAll(Sound.BLOCK_NOTE_BLOCK_PLING));
        addEvent("Geschenk des Unglücks", world -> dropItems(Material.ROTTEN_FLESH, 6));
        addEvent("Rüstungskratzer", world -> damageArmor());
        addEvent("Chaosfunken", world -> spawnParticleCloud(Particle.ELECTRIC_SPARK, 120));
        addEvent("Himmel stürzt", world -> spawnFallingBlocks(Material.STONE, 5));
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

    private void lightningChain(Location origin, int strikes) {
        World world = origin.getWorld();
        for (int i = 0; i < strikes; i++) {
            Location strike = origin.clone().add(randomOffset(6), 0, randomOffset(6));
            strike.setY(world.getHighestBlockYAt(strike) + 1);
            world.strikeLightning(strike);
        }
    }

    private void drainHunger() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            int food = Math.max(0, player.getFoodLevel() - 6);
            player.setFoodLevel(food);
            player.setSaturation(0);
        });
    }

    private void setPlayersOnFire(int seconds) {
        Bukkit.getOnlinePlayers().forEach(player -> player.setFireTicks(seconds * 20));
    }

    private void pullPlayersTogether(int strength) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.size() < 2) {
            return;
        }
        Vector center = new Vector();
        players.forEach(player -> center.add(player.getLocation().toVector()));
        center.multiply(1.0 / players.size());
        players.forEach(player -> {
            Vector direction = center.clone().subtract(player.getLocation().toVector()).normalize();
            player.setVelocity(direction.multiply(strength / 10.0).setY(0.2));
        });
    }

    private void blastPlayersAway() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Vector push = new Vector(randomOffset(2), 1.1, randomOffset(2));
            player.setVelocity(push);
        });
    }

    private void spawnParticleCloud(Particle particle, int count) {
        Bukkit.getOnlinePlayers().forEach(player -> player.getWorld().spawnParticle(
            particle,
            player.getLocation().add(0, 1.2, 0),
            count,
            1.5,
            1.0,
            1.5,
            0.01
        ));
    }

    private void startThunderstorm() {
        World world = getPrimaryWorld();
        world.setStorm(true);
        world.setThundering(true);
        world.setWeatherDuration(20 * 60);
        world.setThunderDuration(20 * 60);
    }

    private void spawnFallingBlocks(Material material, int amount) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            for (int i = 0; i < amount; i++) {
                Location spawn = player.getLocation().clone().add(randomOffset(4), 10 + random.nextInt(4), randomOffset(4));
                FallingBlock block = player.getWorld().spawnFallingBlock(spawn, material.createBlockData());
                block.setDropItem(false);
                block.setHurtEntities(true);
            }
        });
    }

    private void spawnLavaBurst() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1, 0), 30, 0.8, 0.8, 0.8);
            player.setFireTicks(60);
        });
    }

    private void placeThornsAroundPlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location base = player.getLocation();
            for (int i = 0; i < 6; i++) {
                Location spot = base.clone().add(randomOffset(3), 0, randomOffset(3));
                spot.setY(spot.getWorld().getHighestBlockYAt(spot) + 1);
                player.getWorld().spawnEntity(spot, EntityType.EVOKER_FANGS);
            }
        });
    }

    private void playSoundAll(Sound sound) {
        Bukkit.getOnlinePlayers().forEach(player -> player.getWorld().playSound(player.getLocation(), sound, 1.0F, 1.0F));
    }

    private void createSmallExplosions() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            Location base = player.getLocation();
            for (int i = 0; i < 3; i++) {
                Location boom = base.clone().add(randomOffset(3), 0, randomOffset(3));
                player.getWorld().createExplosion(boom, 1.5F, false, false);
            }
        });
    }

    private void damageArmor() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece == null) {
                    continue;
                }
                ItemMeta meta = piece.getItemMeta();
                if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                    damageable.setDamage(damageable.getDamage() + 15);
                    piece.setItemMeta(damageable);
                }
            }
        });
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

    private double randomOffset(int radius) {
        return random.nextInt(radius * 2 + 1) - radius;
    }

    private record EventDefinition(String name, Consumer<World> action) {
    }
}
