package com.example.bingo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BingoLinkPlugin extends JavaPlugin implements Listener {
    private static final double MAX_DISTANCE = 10.0;
    private static final double PULL_STRENGTH = 0.3;
    private static final int PARTICLE_STEPS = 20;

    private final Map<UUID, UUID> links = new HashMap<>();
    private final Map<UUID, Integer> inventoryHashes = new HashMap<>();
    private final Set<UUID> syncingDamage = new HashSet<>();
    private final Set<UUID> syncingFood = new HashSet<>();
    private final Set<UUID> syncingHealth = new HashSet<>();
    private UUID waitingPlayer;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        startSyncTask();
    }

    @Override
    public void onDisable() {
        links.clear();
        inventoryHashes.clear();
        waitingPlayer = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("start")) {
            return false;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Dieser Befehl ist nur für Spieler.");
            return true;
        }
        if (links.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Du bist bereits verbunden.");
            return true;
        }
        if (waitingPlayer != null && !waitingPlayer.equals(player.getUniqueId())) {
            Player other = Bukkit.getPlayer(waitingPlayer);
            if (other != null && other.isOnline()) {
                createLink(player, other);
                waitingPlayer = null;
                return true;
            }
        }
        waitingPlayer = player.getUniqueId();
        player.sendMessage(ChatColor.YELLOW + "Warte auf einen zweiten Spieler mit /start.");
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (waitingPlayer != null && waitingPlayer.equals(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du wartest noch auf einen zweiten Spieler.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        UUID partnerId = links.remove(playerId);
        if (partnerId != null) {
            links.remove(partnerId);
            inventoryHashes.remove(partnerId);
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null) {
                partner.sendMessage(ChatColor.RED + "Deine Verbindung wurde getrennt.");
            }
        }
        if (waitingPlayer != null && waitingPlayer.equals(playerId)) {
            waitingPlayer = null;
        }
        inventoryHashes.remove(playerId);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Player partner = getPartner(player);
        if (partner == null) {
            return;
        }
        if (syncingDamage.contains(player.getUniqueId())) {
            return;
        }
        syncingDamage.add(partner.getUniqueId());
        partner.damage(event.getFinalDamage(), player);
        syncingDamage.remove(partner.getUniqueId());
    }

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Player partner = getPartner(player);
        if (partner == null) {
            return;
        }
        if (syncingHealth.contains(player.getUniqueId())) {
            return;
        }
        syncingHealth.add(partner.getUniqueId());
        double newHealth = Math.min(partner.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(),
                partner.getHealth() + event.getAmount());
        partner.setHealth(newHealth);
        syncingHealth.remove(partner.getUniqueId());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Player partner = getPartner(player);
        if (partner == null) {
            return;
        }
        if (syncingFood.contains(player.getUniqueId())) {
            return;
        }
        syncingFood.add(partner.getUniqueId());
        partner.setFoodLevel(event.getFoodLevel());
        partner.setSaturation(player.getSaturation());
        syncingFood.remove(partner.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player partner = getPartner(player);
        if (partner == null) {
            return;
        }
        partner.setHealth(0.0);
    }

    private void createLink(Player playerOne, Player playerTwo) {
        links.put(playerOne.getUniqueId(), playerTwo.getUniqueId());
        links.put(playerTwo.getUniqueId(), playerOne.getUniqueId());
        playerOne.sendMessage(ChatColor.GREEN + "Du bist jetzt verbunden mit " + playerTwo.getName() + ".");
        playerTwo.sendMessage(ChatColor.GREEN + "Du bist jetzt verbunden mit " + playerOne.getName() + ".");
        syncStatus(playerOne, playerTwo);
    }

    private Player getPartner(Player player) {
        UUID partnerId = links.get(player.getUniqueId());
        if (partnerId == null) {
            return null;
        }
        return Bukkit.getPlayer(partnerId);
    }

    private void startSyncTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, UUID> entry : links.entrySet()) {
                    UUID playerId = entry.getKey();
                    Player player = Bukkit.getPlayer(playerId);
                    Player partner = Bukkit.getPlayer(entry.getValue());
                    if (player == null || partner == null) {
                        continue;
                    }
                    if (playerId.compareTo(partner.getUniqueId()) > 0) {
                        continue;
                    }
                    drawLink(player, partner);
                    enforceDistance(player, partner);
                    syncStatus(player, partner);
                    syncInventory(player, partner);
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void drawLink(Player player, Player partner) {
        Vector start = player.getLocation().toVector().add(new Vector(0, 1.0, 0));
        Vector end = partner.getLocation().toVector().add(new Vector(0, 1.0, 0));
        Vector step = end.clone().subtract(start).multiply(1.0 / PARTICLE_STEPS);
        for (int i = 0; i <= PARTICLE_STEPS; i++) {
            Vector point = start.clone().add(step.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(160, 82, 45), 1.2f));
        }
    }

    private void enforceDistance(Player player, Player partner) {
        double distance = player.getLocation().distance(partner.getLocation());
        if (distance <= MAX_DISTANCE) {
            return;
        }
        Vector toPartner = partner.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
        Vector toPlayer = player.getLocation().toVector().subtract(partner.getLocation().toVector()).normalize();
        player.setVelocity(toPartner.multiply(PULL_STRENGTH));
        partner.setVelocity(toPlayer.multiply(PULL_STRENGTH));
    }

    private void syncStatus(Player player, Player partner) {
        double playerMaxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double partnerMaxHealth = partner.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double targetHealth = Math.min(Math.min(playerMaxHealth, partnerMaxHealth),
                Math.max(player.getHealth(), partner.getHealth()));
        player.setHealth(targetHealth);
        partner.setHealth(targetHealth);

        int targetFood = Math.max(player.getFoodLevel(), partner.getFoodLevel());
        float targetSaturation = Math.max(player.getSaturation(), partner.getSaturation());
        player.setFoodLevel(targetFood);
        partner.setFoodLevel(targetFood);
        player.setSaturation(targetSaturation);
        partner.setSaturation(targetSaturation);
    }

    private void syncInventory(Player player, Player partner) {
        int playerHash = inventoryHash(player.getInventory());
        int partnerHash = inventoryHash(partner.getInventory());
        Integer storedHash = inventoryHashes.get(player.getUniqueId());
        if (storedHash == null) {
            storedHash = playerHash;
        }
        if (playerHash != partnerHash) {
            if (!Objects.equals(playerHash, storedHash) && Objects.equals(partnerHash, storedHash)) {
                copyInventory(player, partner);
                inventoryHashes.put(player.getUniqueId(), playerHash);
                inventoryHashes.put(partner.getUniqueId(), playerHash);
            } else if (!Objects.equals(partnerHash, storedHash) && Objects.equals(playerHash, storedHash)) {
                copyInventory(partner, player);
                inventoryHashes.put(player.getUniqueId(), partnerHash);
                inventoryHashes.put(partner.getUniqueId(), partnerHash);
            } else {
                copyInventory(player, partner);
                inventoryHashes.put(player.getUniqueId(), playerHash);
                inventoryHashes.put(partner.getUniqueId(), playerHash);
            }
        } else {
            inventoryHashes.put(player.getUniqueId(), playerHash);
            inventoryHashes.put(partner.getUniqueId(), partnerHash);
        }
    }

    private void copyInventory(Player source, Player target) {
        PlayerInventory sourceInv = source.getInventory();
        PlayerInventory targetInv = target.getInventory();
        targetInv.setContents(cloneContents(sourceInv.getContents()));
        targetInv.setArmorContents(cloneContents(sourceInv.getArmorContents()));
        targetInv.setItemInOffHand(cloneItem(sourceInv.getItemInOffHand()));
        target.updateInventory();
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private int inventoryHash(PlayerInventory inventory) {
        return Arrays.deepHashCode(new Object[] {
                inventory.getContents(),
                inventory.getArmorContents(),
                inventory.getItemInOffHand()
        });
    }
}
