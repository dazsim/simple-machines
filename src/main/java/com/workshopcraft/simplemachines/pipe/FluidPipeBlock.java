package com.workshopcraft.simplemachines.pipe;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;

/** Copper pipe that carries fluids between tanks and machines, with a window showing what flows. */
public class FluidPipeBlock extends ItemPipeBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);
    public static final int BATCH_MB = 250;
    private static final int SHOW_TICKS = 40;

    public FluidPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FluidPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected Transfer.Lookup lookup() {
        return Transfer.fluidLookup;
    }

    @Override
    protected void pull(PipeNetwork net, ServerLevel level, BlockPos from, Direction side) {
        final Fluid[] seen = new Fluid[1];
        net.pullFluid(level, from, side, BATCH_MB, f -> seen[0] = f);
        if (seen[0] == null) return;
        final long now = level.getGameTime();
        for (BlockPos p : net.pipes()) {
            if (level.getBlockEntity(p) instanceof ConduitBlockEntity be) be.showFluid(seen[0], now, SHOW_TICKS);
        }
    }
}
