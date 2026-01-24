package com.example.bingo;

import dev.emi.pehkui.api.ScaleData;
import dev.emi.pehkui.api.ScaleTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class BingoMod implements ModInitializer {
    public static final String MOD_ID = "bingo";
    public static final Item MAGIC_WAND = new Item(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "magic_wand"), MAGIC_WAND);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerEntity player = handler.player;
            ItemStack wandStack = new ItemStack(MAGIC_WAND);
            if (!player.getInventory().contains(wandStack)) {
                player.getInventory().insertStack(wandStack);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && isHoldingWand(player, hand)) {
                adjustScale(player, -0.1f);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && isHoldingWand(player, hand)) {
                adjustScale(player, 0.1f);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && isHoldingWand(player, hand)) {
                adjustScale(player, 0.1f);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    private static boolean isHoldingWand(PlayerEntity player, Hand hand) {
        return player.getStackInHand(hand).isOf(MAGIC_WAND);
    }

    private static void adjustScale(PlayerEntity player, float delta) {
        ScaleData data = ScaleTypes.BASE.getScaleData(player);
        float currentScale = data.getScale();
        float nextScale = MathHelper.clamp(currentScale + delta, 0.1f, 10.0f);
        data.setScale(nextScale);
    }
}
