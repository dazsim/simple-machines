package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** What goes on a Tool Stand and in which order: frame, core (decides drill or chainsaw), control board, battery module, upgrades. */
public final class ToolParts {
    private ToolParts() {
    }

    public static Map<Item, Item> cores() {
        return Map.of(SimpleMachines.drillMotor, SimpleMachines.TOOLS.get(0), SimpleMachines.chainsawBar, SimpleMachines.TOOLS.get(1));
    }

    public static Item core(@Nullable Item tool) {
        for (Map.Entry<Item, Item> e : cores().entrySet()) if (e.getValue() == tool) return e.getKey();
        return Items.AIR;
    }

    public static Item frameItem(String frame) {
        return "steel".equals(frame) ? SimpleMachines.steelToolFrame : SimpleMachines.ironToolFrame;
    }

    public static ItemStack battery(int energy) {
        final ItemStack stack = new ItemStack(SimpleMachines.batteryModule);
        if (energy > 0) stack.set(SimpleMachines.ENERGY, energy);
        return stack;
    }

    /** Sits a part loosely on the stand, or mounts a finished tool on a bare one. Null when the stack is not a tool part. */
    public static @Nullable InteractionResult fit(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof ToolStandBlockEntity be)) return null;
        final int step = be.step();
        final Upgrade upgrade = Upgrade.of(stack);
        final boolean tool = stack.getItem() instanceof PoweredToolItem;
        final boolean frame = stack.is(SimpleMachines.ironToolFrame) || stack.is(SimpleMachines.steelToolFrame);
        final Item core = cores().get(stack.getItem());
        final boolean board = stack.is(SimpleMachines.controlBoard);
        final boolean battery = stack.is(SimpleMachines.batteryModule);
        if (!tool && !frame && core == null && !board && !battery && upgrade == null) return null;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        String msg = null;
        if (tool) {
            if (step == 0) {
                be.mount(stack);
                stack.consume(1, player);
            } else msg = "message.simplemachines.stand_busy";
        } else if (upgrade != null) {
            if (step < ToolStandBlockEntity.DONE) msg = step % 2 == 1 ? "message.simplemachines.loose" : "message.simplemachines.tool_upgrade_early";
            else if (be.loose() != null) msg = "message.simplemachines.loose";
            else if (be.upgrades().size() >= Upgrade.SLOTS) msg = "message.simplemachines.upgrades_full";
            else {
                be.setLoose(upgrade);
                stack.consume(1, player);
            }
        } else if (frame && step == 0) {
            be.setFrame(stack.is(SimpleMachines.steelToolFrame) ? "steel" : "iron");
            stack.consume(1, player);
        } else if (core != null && step == 2) {
            be.setTool(core);
            stack.consume(1, player);
        } else if (board && step == 4) {
            be.setStep(5);
            stack.consume(1, player);
        } else if (battery && step == 6) {
            be.setBattery(PoweredToolItem.energy(stack));
            stack.consume(1, player);
        } else {
            msg = hint(be);
        }
        if (msg != null) player.sendOverlayMessage(Component.translatable(msg));
        else level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    public static String hint(ToolStandBlockEntity be) {
        if (be.loose() != null || be.step() % 2 == 1) return "message.simplemachines.loose";
        return switch (be.step()) {
            case 0 -> "message.simplemachines.needs_tool_frame";
            case 2 -> "message.simplemachines.needs_tool_core";
            case 4 -> "message.simplemachines.needs_board";
            case 6 -> "message.simplemachines.needs_battery_module";
            default -> "message.simplemachines.tool_ready";
        };
    }

    /** Empty hand: lifts a finished tool off the stand. */
    public static InteractionResult take(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ToolStandBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!be.complete()) {
            player.sendOverlayMessage(Component.translatable(be.step() == 0 ? "message.simplemachines.needs_tool_frame" : "message.simplemachines.tool_unfinished"));
            return InteractionResult.SUCCESS;
        }
        final ItemStack tool = be.take();
        if (!player.getInventory().add(tool)) player.drop(tool, false);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Screwdriver: screws in the loose part, or with sneak takes the outermost one off. */
    public static void screwdriver(Level level, BlockPos pos, Player player, boolean sneak) {
        if (!(level.getBlockEntity(pos) instanceof ToolStandBlockEntity be)) return;
        if (sneak) {
            final ItemStack part = be.strip();
            if (part.isEmpty()) return;
            Block.popResource(level, pos.above(), part);
        } else {
            final boolean upgrade = be.loose() != null;
            if (!be.fix()) {
                player.sendOverlayMessage(Component.translatable(hint(be)));
                return;
            }
            if (upgrade) player.sendOverlayMessage(Component.translatable("message.simplemachines.upgrade_fitted", be.upgrades().size(), Upgrade.SLOTS));
            else if (be.complete()) player.sendOverlayMessage(Component.translatable("message.simplemachines.tool_ready"));
        }
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
    }
}
