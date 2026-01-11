package com.bingo.missilewars;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MissileWarsRocketListener implements Listener {
    private final MissileWarsPlugin plugin;

    public MissileWarsRocketListener(MissileWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerUseEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        RocketType type = MissileItemFactory.getRocketType(plugin, item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        Location spawnLocation = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        type.launch(event.getPlayer(), spawnLocation);
        consumeOne(item);
    }

    private void consumeOne(ItemStack item) {
        if (item == null) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            item.setAmount(0);
        } else {
            item.setAmount(amount - 1);
        }
    }
}
