package com.example.bingo.sizewand;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.block.Action;

public class SizeWandPlugin extends JavaPlugin implements Listener {
    private static final double SCALE_STEP = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;

    private NamespacedKey wandKey;

    @Override
    public void onEnable() {
        wandKey = new NamespacedKey(this, "size_wand");
        Bukkit.getPluginManager().registerEvents(this, this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            giveWandIfMissing(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        giveWandIfMissing(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isWand(item)) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            adjustScale(event.getPlayer(), -SCALE_STEP);
            event.setCancelled(true);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            adjustScale(event.getPlayer(), SCALE_STEP);
            event.setCancelled(true);
        }
    }

    private void adjustScale(Player player, double delta) {
        AttributeInstance scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttribute == null) {
            return;
        }

        double current = scaleAttribute.getBaseValue();
        double updated = Math.max(MIN_SCALE, Math.min(MAX_SCALE, current + delta));
        scaleAttribute.setBaseValue(updated);
    }

    private void giveWandIfMissing(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (isWand(stack)) {
                return;
            }
        }

        inventory.addItem(createWand());
    }

    private boolean isWand(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.BLAZE_ROD) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    private ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = Objects.requireNonNull(wand.getItemMeta());
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Groessen-Zauberstab");
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        wand.setItemMeta(meta);
        return wand;
    }
}
