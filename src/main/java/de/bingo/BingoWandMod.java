package de.bingo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

import java.util.Collection;

public class BingoWandMod implements ModInitializer {
    public static final String MOD_ID = "bingo";
    private static final String WAND_TAG = "bingo_wand";
    private static final float SCALE_STEP = 0.1f;

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (!hasWand(player)) {
                giveWand(player);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!isWand(stack)) {
                return TypedActionResult.pass(stack);
            }
            if (world.isClient()) {
                return TypedActionResult.pass(stack);
            }
            adjustScale(player, -SCALE_STEP);
            return TypedActionResult.success(stack, false);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!shouldHandleAttack(player, world, hand)) {
                return ActionResult.PASS;
            }
            adjustScale(player, SCALE_STEP);
            return ActionResult.SUCCESS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!shouldHandleAttack(player, world, hand)) {
                return ActionResult.PASS;
            }
            adjustScale(player, SCALE_STEP);
            return ActionResult.SUCCESS;
        });

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("zauberstab")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(context -> {
                giveWand(context.getSource().getPlayerOrThrow());
                return 1;
            })
            .then(CommandManager.argument("targets", EntityArgumentType.players())
                .executes(context -> {
                    Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "targets");
                    for (ServerPlayerEntity target : targets) {
                        giveWand(target);
                    }
                    return targets.size();
                }))
        );
    }

    private static boolean shouldHandleAttack(PlayerEntity player, World world, Hand hand) {
        if (world.isClient()) {
            return false;
        }
        return isWand(player.getStackInHand(hand));
    }

    private static void giveWand(ServerPlayerEntity player) {
        ItemStack wand = createWand();
        if (!player.getInventory().insertStack(wand)) {
            player.dropItem(wand, false);
        }
    }

    private static boolean hasWand(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isWand(stack)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack createWand() {
        ItemStack stack = new ItemStack(Items.CARROT_ON_A_STICK);
        stack.setCustomName(Text.literal("Zauberstab").styled(style -> style.withItalic(false).withColor(Formatting.LIGHT_PURPLE)));
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(WAND_TAG, true);
        return stack;
    }

    private static boolean isWand(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != Items.CARROT_ON_A_STICK) {
            return false;
        }
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(WAND_TAG);
    }

    private static void adjustScale(Entity player, float delta) {
        ScaleData data = ScaleTypes.BASE.getScaleData(player);
        float next = data.getScale() + delta;
        if (next < 0.1f) {
            next = 0.1f;
        }
        data.setScale(next);
    }
}
