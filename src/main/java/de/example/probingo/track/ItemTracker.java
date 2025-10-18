package de.example.probingo.track;

import de.example.probingo.game.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;

public final class ItemTracker implements Listener {

    private final GameManager gameManager;

    public ItemTracker(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        handle(player, event.getItem().getItemStack(), AcquisitionSource.PICKUP);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        handle(player, event.getRecipe().getResult(), AcquisitionSource.CRAFT);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        ItemStack result = new ItemStack(event.getItemType(), event.getItemAmount());
        handle(player, result, AcquisitionSource.SMELT);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof org.bukkit.entity.Item item)) {
            return;
        }
        Player player = event.getPlayer();
        handle(player, item.getItemStack(), AcquisitionSource.FISH);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        event.getItems().stream()
                .map(org.bukkit.entity.Item::getItemStack)
                .forEach(stack -> handle(player, stack, AcquisitionSource.DROP));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        if (top.getType() == InventoryType.MERCHANT) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                handle(player, current, AcquisitionSource.TRADE);
            }
            if (event.getClickedInventory() instanceof MerchantInventory && event.getSlotType() == InventoryType.SlotType.RESULT) {
                handle(player, current, AcquisitionSource.TRADE);
            }
            return;
        }
        if (top.getType() != InventoryType.PLAYER) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                handle(player, current, AcquisitionSource.LOOT);
            }
            switch (event.getAction()) {
                case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> handle(player, current, AcquisitionSource.LOOT);
                default -> {
                }
            }
        }
    }

    private void handle(Player player, ItemStack stack, AcquisitionSource source) {
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        gameManager.handleItemAcquired(player, stack, source);
    }
}
