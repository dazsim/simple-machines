package com.workshopcraft.simplemachines.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Plain energy store; each loader wraps it in its own energy API. */
public class EnergyBuffer {
    /** Set by the loader: offers energy to the block at pos from side, returns the amount taken. */
    public static Pusher pusher = (level, pos, side, amount) -> 0;

    public interface Pusher {
        int push(ServerLevel level, BlockPos pos, Direction side, int amount);
    }

    /** Set by the loader: whether the block at pos exposes energy on side. Used by cables to decide connections. */
    public static Lookup lookup = (level, pos, side) -> false;

    public interface Lookup {
        boolean has(Level level, BlockPos pos, Direction side);
    }

    public int capacity;
    public int maxIn;
    public int maxOut;
    public int energy;
    private final Runnable onChange;

    public EnergyBuffer(int capacity, int maxIn, int maxOut, Runnable onChange) {
        this.capacity = capacity;
        this.maxIn = maxIn;
        this.maxOut = maxOut;
        this.onChange = onChange;
    }

    public void changed() {
        onChange.run();
    }

    public void pushToNeighbours(ServerLevel level, BlockPos pos) {
        int left = Math.min(energy, maxOut);
        for (Direction d : Direction.values()) {
            if (left <= 0) break;
            final int n = pusher.push(level, pos.relative(d), d.getOpposite(), left);
            left -= n;
            energy -= n;
        }
    }
}
