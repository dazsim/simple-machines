package com.workshopcraft.simplemachines.cable;

import com.workshopcraft.simplemachines.pipe.ItemPipeBlock;
import com.workshopcraft.simplemachines.pipe.PipeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Dyeing and the cable screwdriver, shared by cables and item pipes. */
public final class Conduits {
    private Conduits() {
    }

    public interface Recompute {
        BlockState recompute(Level level, BlockPos pos, BlockState state);
    }

    public static InteractionResult dye(ItemStack held, BlockState state, Level level, BlockPos pos, Player player) {
        final DyeColor c = held.get(net.minecraft.core.component.DataComponents.DYE);
        if (c == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity)) {
            level.setBlockEntity(new ConduitBlockEntity(pos, state));
        }
        if (!(level.getBlockEntity(pos) instanceof ConduitBlockEntity be) || be.colour() == c) return InteractionResult.SUCCESS;
        be.setColour(c);
        if (!player.getAbilities().instabuild) held.shrink(1);
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1F, 1F);
        refresh(level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Screwdriver on a cable: switches the side nearest the click on or off. */
    public static boolean screwdriver(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CableBlock)) return false;
        if (!(level instanceof ServerLevel server)) return true;
        final Vec3 p = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Direction side = hit.getDirection();
        double score = -1e9;
        for (Direction d : Direction.values()) {
            final double s = p.x * d.getStepX() + p.y * d.getStepY() + p.z * d.getStepZ();
            if (s > score) {
                score = s;
                side = d;
            }
        }
        if (!(server.getBlockEntity(pos) instanceof ConduitBlockEntity)) server.setBlockEntity(new ConduitBlockEntity(pos, state));
        if (!(server.getBlockEntity(pos) instanceof ConduitBlockEntity be)) return true;
        final boolean on = be.toggle(side);
        refresh(server, pos);
        player.sendOverlayMessage(Component.translatable(on ? "message.simplemachines.cable_on" : "message.simplemachines.cable_off",
                Component.translatable("direction.simplemachines." + side.getSerializedName())));
        server.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, on ? 1.2F : 0.9F);
        return true;
    }

    /** Works out the sides of a conduit and its neighbours again after a colour or side change. */
    public static void refresh(Level level, BlockPos pos) {
        update(level, pos);
        for (Direction d : Direction.values()) update(level, pos.relative(d));
        if (level instanceof ServerLevel server) {
            CableNetwork.invalidate(server);
            PipeNetwork.invalidate(server);
        }
    }

    private static void update(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Recompute r)) return;
        final BlockState next = r.recompute(level, pos, state);
        if (next != state) level.setBlock(pos, next, Block.UPDATE_ALL);
    }

    public static boolean isConduit(Block b) {
        return b instanceof CableBlock || b instanceof ItemPipeBlock;
    }
}
