package com.example.bingo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final int PARTICLE_STEPS = 10;
    private static final float PARTICLE_SIZE = 0.6f;

    private final Map<UUID, Set<UUID>> groups = new HashMap<>();
    private final Map<UUID, UUID> playerGroups = new HashMap<>();
    private final List<UUID> waitingPlayers = new ArrayList<>();
    private final Set<UUID> syncingDamage = new HashSet<>();
    private final Set<UUID> syncingFood = new HashSet<>();
    private final Set<UUID> syncingHealth = new HashSet<>();
    private final Set<UUID> syncingDeath = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        startSyncTask();
    }

    @Override
    public void onDisable() {
        groups.clear();
        playerGroups.clear();
        waitingPlayers.clear();
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
        if (playerGroups.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Du bist bereits verbunden.");
            return true;
        }
        if (waitingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Du wartest bereits auf weitere Spieler.");
            return true;
        }
        int groupSize = Math.max(2, getConfig().getInt("max-linked-players", 2));
        waitingPlayers.add(player.getUniqueId());
        int waitingCount = waitingPlayers.size();
        if (waitingCount < groupSize) {
            player.sendMessage(ChatColor.YELLOW + "Warte auf weitere Spieler (" + waitingCount + "/" + groupSize + ").");
            return true;
        }
        List<Player> readyPlayers = new ArrayList<>();
        List<UUID> consumed = new ArrayList<>();
        for (UUID id : waitingPlayers) {
            Player candidate = Bukkit.getPlayer(id);
            if (candidate != null && candidate.isOnline() && !playerGroups.containsKey(id)) {
                readyPlayers.add(candidate);
                consumed.add(id);
            }
            if (readyPlayers.size() == groupSize) {
                break;
            }
        }
        waitingPlayers.removeAll(consumed);
        if (readyPlayers.size() < groupSize) {
            player.sendMessage(ChatColor.YELLOW + "Es fehlen noch Spieler für die Verbindung.");
            return true;
        }
        createGroup(readyPlayers);
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (waitingPlayers.contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Du wartest noch auf weitere Spieler.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        waitingPlayers.remove(playerId);
        UUID groupId = playerGroups.remove(playerId);
        if (groupId == null) {
            return;
        }
        Set<UUID> members = groups.remove(groupId);
        if (members == null) {
            return;
        }
        for (UUID memberId : members) {
            playerGroups.remove(memberId);
            if (!memberId.equals(playerId)) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    member.sendMessage(ChatColor.RED + "Deine Verbindung wurde getrennt.");
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID groupId = playerGroups.get(player.getUniqueId());
        if (groupId == null) {
            return;
        }
        if (syncingDamage.contains(player.getUniqueId())) {
            return;
        }
        Set<UUID> members = groups.get(groupId);
        if (members == null) {
            return;
        }
        for (UUID memberId : members) {
            if (memberId.equals(player.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }
            syncingDamage.add(memberId);
            member.damage(event.getFinalDamage(), player);
            syncingDamage.remove(memberId);
        }
    }

    @EventHandler
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID groupId = playerGroups.get(player.getUniqueId());
        if (groupId == null) {
            return;
        }
        if (syncingHealth.contains(player.getUniqueId())) {
            return;
        }
        Set<UUID> members = groups.get(groupId);
        if (members == null) {
            return;
        }
        for (UUID memberId : members) {
            if (memberId.equals(player.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }
            syncingHealth.add(memberId);
            double newHealth = Math.min(member.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(),
                    member.getHealth() + event.getAmount());
            member.setHealth(newHealth);
            syncingHealth.remove(memberId);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID groupId = playerGroups.get(player.getUniqueId());
        if (groupId == null) {
            return;
        }
        if (syncingFood.contains(player.getUniqueId())) {
            return;
        }
        Set<UUID> members = groups.get(groupId);
        if (members == null) {
            return;
        }
        for (UUID memberId : members) {
            if (memberId.equals(player.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }
            syncingFood.add(memberId);
            member.setFoodLevel(event.getFoodLevel());
            member.setSaturation(player.getSaturation());
            syncingFood.remove(memberId);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID groupId = playerGroups.get(player.getUniqueId());
        if (groupId == null) {
            return;
        }
        if (syncingDeath.contains(player.getUniqueId())) {
            return;
        }
        Set<UUID> members = groups.get(groupId);
        if (members == null) {
            return;
        }
        for (UUID memberId : members) {
            if (memberId.equals(player.getUniqueId())) {
                continue;
            }
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }
            syncingDeath.add(memberId);
            member.setHealth(0.0);
            syncingDeath.remove(memberId);
        }
    }

    private void createGroup(List<Player> players) {
        UUID groupId = players.stream()
                .map(Player::getUniqueId)
                .min(UUID::compareTo)
                .orElseThrow();
        Set<UUID> members = new HashSet<>();
        for (Player player : players) {
            members.add(player.getUniqueId());
            playerGroups.put(player.getUniqueId(), groupId);
        }
        groups.put(groupId, members);
        for (Player player : players) {
            player.sendMessage(ChatColor.GREEN + "Du bist jetzt verbunden mit " + groupNames(player, players) + ".");
        }
        syncStatus(players);
    }

    private String groupNames(Player self, List<Player> players) {
        List<String> names = new ArrayList<>();
        for (Player player : players) {
            if (!player.getUniqueId().equals(self.getUniqueId())) {
                names.add(player.getName());
            }
        }
        return String.join(", ", names);
    }

    private void startSyncTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Set<UUID>> entry : groups.entrySet()) {
                    List<Player> players = getOnlinePlayers(entry.getValue());
                    if (players.size() < 2) {
                        continue;
                    }
                    drawLinks(players);
                    enforceDistance(players);
                    syncStatus(players);
                    syncInventory(players);
                }
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private List<Player> getOnlinePlayers(Set<UUID> members) {
        List<Player> players = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private void drawLinks(List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                drawLink(players.get(i), players.get(j));
            }
        }
    }

    private void drawLink(Player player, Player partner) {
        Vector start = player.getLocation().toVector().add(new Vector(0, 1.0, 0));
        Vector end = partner.getLocation().toVector().add(new Vector(0, 1.0, 0));
        Vector step = end.clone().subtract(start).multiply(1.0 / PARTICLE_STEPS);
        for (int i = 0; i <= PARTICLE_STEPS; i++) {
            Vector point = start.clone().add(step.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(160, 82, 45), PARTICLE_SIZE));
        }
    }

    private void enforceDistance(List<Player> players) {
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                enforceDistance(players.get(i), players.get(j));
            }
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

    private void syncStatus(List<Player> players) {
        double maxHealth = 0.0;
        double minMaxHealth = Double.MAX_VALUE;
        int maxFood = 0;
        float maxSaturation = 0.0f;
        for (Player player : players) {
            maxHealth = Math.max(maxHealth, player.getHealth());
            double playerMaxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            minMaxHealth = Math.min(minMaxHealth, playerMaxHealth);
            maxFood = Math.max(maxFood, player.getFoodLevel());
            maxSaturation = Math.max(maxSaturation, player.getSaturation());
        }
        double targetHealth = Math.min(minMaxHealth, maxHealth);
        for (Player player : players) {
            player.setHealth(targetHealth);
            player.setFoodLevel(maxFood);
            player.setSaturation(maxSaturation);
        }
    }

    private void syncInventory(List<Player> players) {
        Player source = players.stream()
                .min((a, b) -> a.getUniqueId().compareTo(b.getUniqueId()))
                .orElse(null);
        if (source == null) {
            return;
        }
        int sourceHash = inventoryHash(source.getInventory());
        boolean needsSync = false;
        for (Player player : players) {
            if (player.getUniqueId().equals(source.getUniqueId())) {
                continue;
            }
            if (inventoryHash(player.getInventory()) != sourceHash) {
                needsSync = true;
                break;
            }
        }
        if (!needsSync) {
            return;
        }
        for (Player player : players) {
            if (!player.getUniqueId().equals(source.getUniqueId())) {
                copyInventory(source, player);
            }
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
