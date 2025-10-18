package de.example.probingo.gui;

import de.example.probingo.game.card.BingoCard;
import de.example.probingo.game.card.BingoCell;
import de.example.probingo.util.MaterialNameFormatter;
import de.example.probingo.util.TimeFormats;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class CardView implements Listener {

    private static final int[] CARD_SLOTS = {
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23,
            28, 29, 30, 31, 32,
            37, 38, 39, 40, 41,
            46, 47, 48, 49, 50
    };

    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    private final Map<Inventory, UUID> inventoryOwners = new ConcurrentHashMap<>();
    private final Map<UUID, BingoCard> cards = new ConcurrentHashMap<>();

    public void open(Player player, BingoCard card) {
        Inventory inventory = Bukkit.createInventory(player, 54, Component.text("§b§lBINGO – 5×5"));
        fillFrame(inventory);
        openInventories.put(player.getUniqueId(), inventory);
        inventoryOwners.put(inventory, player.getUniqueId());
        cards.put(player.getUniqueId(), card);
        updateInventory(inventory, card);
        player.openInventory(inventory);
    }

    public void updateFor(Player player) {
        Inventory inventory = openInventories.get(player.getUniqueId());
        BingoCard card = cards.get(player.getUniqueId());
        if (inventory == null || card == null) {
            return;
        }
        updateInventory(inventory, card);
    }

    public void updateForIds(Iterable<UUID> players, BingoCard card) {
        for (UUID playerId : players) {
            Inventory inventory = openInventories.get(playerId);
            if (inventory == null) {
                continue;
            }
            updateInventory(inventory, card);
        }
    }

    public void closeAll() {
        for (UUID uuid : openInventories.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getOpenInventory().getType() == InventoryType.CHEST) {
                player.closeInventory();
            }
        }
        openInventories.clear();
        inventoryOwners.clear();
        cards.clear();
    }

    private void fillFrame(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text(" "));
        filler.setItemMeta(meta);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        ItemStack legend = new ItemStack(Material.PAPER);
        ItemMeta legendMeta = legend.getItemMeta();
        legendMeta.displayName(Component.text("§7Linksklick: Details"));
        legendMeta.lore(List.of(Component.text("§7Rechtsklick: Nichts")));
        legend.setItemMeta(legendMeta);
        inventory.setItem(53, legend);
    }

    private void updateInventory(Inventory inventory, BingoCard card) {
        List<BingoCell> cells = card.cells();
        for (int i = 0; i < CARD_SLOTS.length; i++) {
            BingoCell cell = cells.get(i);
            inventory.setItem(CARD_SLOTS[i], createItem(cell));
        }
    }

    private ItemStack createItem(BingoCell cell) {
        ItemStack stack = new ItemStack(cell.material());
        ItemMeta meta = stack.getItemMeta();
        String name = MaterialNameFormatter.displayName(cell.material());
        if (cell.isCompleted()) {
            meta.displayName(Component.text("§a✔ " + name));
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.displayName(Component.text("§f" + name));
        }
        meta.lore(buildLore(cell));
        stack.setItemMeta(meta);
        return stack;
    }

    private List<Component> buildLore(BingoCell cell) {
        if (!cell.isCompleted()) {
            return List.of(
                    Component.text("§7Status: §cNicht erhalten"),
                    Component.text("§7Quelle: §f-"),
                    Component.text("§7Zeit: §f--:--")
            );
        }
        String source = cell.source() != null ? cell.source().displayKey() : "pickup";
        String timestamp = TimeFormats.timestamp(cell.timestamp());
        return List.of(
                Component.text("§7Status: §aErhalten"),
                Component.text("§7Quelle: §f" + source),
                Component.text("§7Zeit: §f" + timestamp)
        );
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!inventoryOwners.containsKey(inventory)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        UUID owner = inventoryOwners.remove(inventory);
        if (owner != null) {
            openInventories.remove(owner);
            cards.remove(owner);
        }
    }
}
