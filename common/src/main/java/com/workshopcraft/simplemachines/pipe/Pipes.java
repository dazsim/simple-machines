package com.workshopcraft.simplemachines.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class Pipes {
    private Pipes() {
    }

    /** Screwdriver on a pipe: cycles the inventory end nearest the click through input, output and disconnected. */
    public static boolean screwdriver(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ItemPipeBlock)) return false;
        final Direction d = nearestInventoryArm(state, pos, hit);
        if (d == null) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.simplemachines.pipe_no_inventory"));
            return true;
        }
        if (level instanceof ServerLevel server) {
            final EnumProperty<Link> p = ItemPipeBlock.LINKS.get(d);
            final Link next = state.getValue(p).cycle();
            final BlockState updated = state.setValue(p, next);
            server.setBlock(pos, updated, 3);
            PipeNetwork.invalidate(server);
            if (next == Link.EXTRACT && !server.getBlockTicks().hasScheduledTick(pos, updated.getBlock())) {
                server.scheduleTick(pos, updated.getBlock(), ItemPipeBlock.INTERVAL);
            }
            player.sendOverlayMessage(Component.translatable("message.simplemachines.pipe_" + next.getSerializedName()));
            server.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return true;
    }

    private static @Nullable Direction nearestInventoryArm(BlockState state, BlockPos pos, BlockHitResult hit) {
        final Vec3 p = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction best = null;
        double score = -1e9;
        for (Direction d : Direction.values()) {
            if (!state.getValue(ItemPipeBlock.LINKS.get(d)).end()) continue;
            final double s = p.x * d.getStepX() + p.y * d.getStepY() + p.z * d.getStepZ();
            if (s > score) {
                score = s;
                best = d;
            }
        }
        return best;
    }
}
