package com.workshopcraft.simplemachines.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Item moving hooks, filled in by each loader with its own item API. */
public final class Transfer {
    private Transfer() {
    }

    public interface Lookup {
        boolean has(Level level, BlockPos pos, Direction side);
    }

    public interface Mover {
        /** Moves up to max items out of the block at from (seen from fromSide) into the block at to (seen from toSide). */
        int move(ServerLevel level, BlockPos from, Direction fromSide, BlockPos to, Direction toSide, int max);
    }

    public interface Invalidator {
        void invalidate(ServerLevel level, BlockPos pos);
    }

    public static Lookup lookup = (level, pos, side) -> false;
    public static Mover mover = (level, from, fromSide, to, toSide, max) -> 0;
    public static Invalidator invalidator = (level, pos) -> {};
}
