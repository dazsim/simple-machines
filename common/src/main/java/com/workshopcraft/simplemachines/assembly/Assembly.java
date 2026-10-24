package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.MachineBlock;
import com.workshopcraft.simplemachines.machine.PoweredMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.workshopcraft.simplemachines.assembly.ChassisBlock.CORE;
import static com.workshopcraft.simplemachines.assembly.ChassisBlock.PANEL;
import static com.workshopcraft.simplemachines.assembly.ChassisBlock.STEP;

public final class Assembly {
    private Assembly() {
    }

    /** Sits a part loosely on the chassis. Returns null when the stack is not a machine part. */
    public static @Nullable InteractionResult fit(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack) {
        final int step = state.getValue(STEP);
        final Core core = Core.of(stack);
        final Finish panels = Finish.panelsOf(stack);
        final String worktop = Worktops.of(stack);
        final boolean board = stack.is(SimpleMachines.controlBoard);
        if (core == Core.NONE && panels == Finish.NONE && worktop.isEmpty() && !board) return null;

        BlockState next = null;
        if (core != Core.NONE && step == 0) next = state.setValue(CORE, core).setValue(STEP, 1);
        else if (board && step == 2) next = state.setValue(STEP, 3);
        else if (panels != Finish.NONE && step == 4) next = state.setValue(PANEL, panels).setValue(STEP, 5);
        else if (!worktop.isEmpty() && step == 6) next = state.setValue(STEP, 7);

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (next == null) {
            player.sendOverlayMessage(Component.translatable(hint(step)));
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (!worktop.isEmpty() && level.getBlockEntity(pos) instanceof Worktops.Holder h) h.setWorktop(worktop);
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 0.9F);
        return InteractionResult.SUCCESS;
    }

    private static String hint(int step) {
        return switch (step) {
            case 0 -> "message.simplemachines.needs_assembly";
            case 2 -> "message.simplemachines.needs_board";
            case 4 -> "message.simplemachines.needs_panels";
            case 6 -> "message.simplemachines.needs_worktop";
            default -> "message.simplemachines.loose";
        };
    }

    /** Screws the loose part in. The worktop is last; fixing it turns the chassis into its machine. */
    public static boolean screw(BlockState state, Level level, BlockPos pos, Player player) {
        final int step = state.getValue(STEP);
        if (step % 2 == 0) {
            player.sendOverlayMessage(Component.translatable(hint(step)));
            return false;
        }
        if (step == 7) {
            final Tier tier = ((ChassisBlock) state.getBlock()).tier;
            final String worktop = worktopAt(level, pos);
            level.setBlock(pos, state.getValue(CORE).machine().defaultBlockState()
                    .setValue(MachineBlock.FACING, state.getValue(ChassisBlock.FACING))
                    .setValue(PoweredMachineBlock.TIER, tier)
                    .setValue(PoweredMachineBlock.PANEL, state.getValue(PANEL)), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof Worktops.Holder h) h.setWorktop(worktop);
        } else {
            level.setBlock(pos, state.setValue(STEP, step + 1), Block.UPDATE_ALL);
        }
        ratchet(level, pos);
        return true;
    }

    /** Takes the outermost part off, fixed or loose. */
    public static boolean unscrew(BlockState state, Level level, BlockPos pos) {
        final int step = state.getValue(STEP);
        if (step == 0) return false;
        final ItemStack part;
        final BlockState next;
        if (step == 7) {
            part = Worktops.stack(worktopAt(level, pos));
            if (level.getBlockEntity(pos) instanceof Worktops.Holder h) h.setWorktop("");
            next = state.setValue(STEP, 6);
        } else if (step >= 5) {
            part = new ItemStack(state.getValue(PANEL).panels());
            next = state.setValue(PANEL, Finish.NONE).setValue(STEP, 4);
        } else if (step >= 3) {
            part = new ItemStack(SimpleMachines.controlBoard);
            next = state.setValue(STEP, 2);
        } else {
            part = new ItemStack(state.getValue(CORE).item());
            next = state.setValue(CORE, Core.NONE).setValue(STEP, 0);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        Block.popResource(level, pos.above(), part);
        ratchet(level, pos);
        return true;
    }

    /** Opens a finished machine back up: contents drop, the worktop comes off, the rest stays on the chassis. */
    public static void disassemble(BlockState state, Level level, BlockPos pos) {
        final PoweredMachineBlock block = (PoweredMachineBlock) state.getBlock();
        final String worktop = worktopAt(level, pos);
        level.setBlock(pos, state.getValue(PoweredMachineBlock.TIER).chassis().defaultBlockState()
                .setValue(ChassisBlock.FACING, state.getValue(MachineBlock.FACING))
                .setValue(CORE, block.core())
                .setValue(PANEL, state.getValue(PoweredMachineBlock.PANEL))
                .setValue(STEP, 6), Block.UPDATE_ALL);
        Block.popResource(level, pos.above(), Worktops.stack(worktop));
        ratchet(level, pos);
    }

    private static String worktopAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Worktops.Holder h ? h.worktop() : "";
    }

    /** Any c:tools/screwdriver: screws parts in, sneak takes the last one off or opens a machine back up. */
    public static boolean screwdriver(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!held.is(Worktops.SCREWDRIVERS) || !player.mayBuild()) return false;
        final BlockState state = level.getBlockState(pos);
        final boolean sneak = player.isSecondaryUseActive();
        if (state.getBlock() instanceof ChassisBlock) {
            if (!level.isClientSide()) {
                if (sneak) unscrew(state, level, pos);
                else screw(state, level, pos, player);
            }
            return true;
        }
        if (state.getBlock() instanceof PoweredMachineBlock && sneak) {
            if (!level.isClientSide()) disassemble(state, level, pos);
            return true;
        }
        return false;
    }

    public static List<ItemStack> parts(BlockState state, String worktop) {
        final int step = state.getValue(STEP);
        final List<ItemStack> parts = new ArrayList<>();
        if (step >= 1) parts.add(new ItemStack(state.getValue(CORE).item()));
        if (step >= 3) parts.add(new ItemStack(SimpleMachines.controlBoard));
        if (step >= 5) parts.add(new ItemStack(state.getValue(PANEL).panels()));
        if (step >= 7 && !worktop.isEmpty()) parts.add(Worktops.stack(worktop));
        return parts;
    }

    private static void ratchet(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.5F, 1.6F);
    }
}
