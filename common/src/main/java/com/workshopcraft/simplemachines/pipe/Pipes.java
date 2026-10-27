package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public final class Pipes {
    private Pipes() {
    }

    /** Screwdriver on a pipe: cycles the inventory end nearest the click through input, output and disconnected. */
    public static boolean screwdriver(Level level, BlockPos pos, Player player, ItemStack held, BlockHitResult hit) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ItemPipeBlock)) return false;
        final Direction d = nearest(pos, hit, s -> state.getValue(ItemPipeBlock.LINKS.get(s)).end());
        if (d == null) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable(state.getBlock() instanceof FluidPipeBlock
                    ? "message.simplemachines.pipe_no_fluid" : "message.simplemachines.pipe_no_inventory"));
            return true;
        }
        if (level instanceof ServerLevel server) {
            final EnumProperty<Link> p = ItemPipeBlock.LINKS.get(d);
            final Link next = state.getValue(p).cycle();
            if (server.getBlockEntity(pos) instanceof ConduitBlockEntity be) Block.popResource(server, pos, be.removeFilter(d));
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

    /** Filter in hand on a pipe: fits it to the input end nearest the click. */
    public static InteractionResult fitFilter(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        final Direction d = nearest(pos, hit, s -> state.getValue(ItemPipeBlock.LINKS.get(s)) == Link.LINK);
        if (d == null) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.simplemachines.filter_no_end"));
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity be)) return InteractionResult.PASS;
        if (be.hasFilter(d)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.simplemachines.filter_present"));
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            be.setFilter(d, held.getOrDefault(SimpleMachines.FILTER, PipeFilter.EMPTY));
            held.consume(1, player);
            server.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
            player.sendOverlayMessage(Component.translatable("message.simplemachines.filter_fitted"));
        }
        return InteractionResult.SUCCESS;
    }

    /** Empty hand on a filtered end opens its settings. */
    public static InteractionResult openFilter(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof ConduitBlockEntity be)) return InteractionResult.PASS;
        final Direction d = nearest(pos, hit, s -> be.hasFilter(s) && state.getValue(ItemPipeBlock.LINKS.get(s)) == Link.LINK);
        if (d == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, be, d),
                    Component.translatable("container.simplemachines.pipe_filter")));
        }
        return InteractionResult.SUCCESS;
    }

    /** Pops the filter off any end that stopped delivering into a block. */
    public static void dropLostFilters(ServerLevel level, BlockPos pos, BlockState old, BlockState now) {
        if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity be) || be.filterMask() == 0) return;
        for (Direction d : Direction.values()) {
            if (be.hasFilter(d) && ItemPipeBlock.link(now, d) != Link.LINK) Block.popResource(level, pos, be.removeFilter(d));
        }
    }

    private static @Nullable Direction nearest(BlockPos pos, BlockHitResult hit, Predicate<Direction> allowed) {
        final Vec3 p = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction best = null;
        double score = -1e9;
        for (Direction d : Direction.values()) {
            if (!allowed.test(d)) continue;
            final double s = p.x * d.getStepX() + p.y * d.getStepY() + p.z * d.getStepZ();
            if (s > score) {
                score = s;
                best = d;
            }
        }
        return best;
    }
}
