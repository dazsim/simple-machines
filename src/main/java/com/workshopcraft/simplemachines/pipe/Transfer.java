package com.workshopcraft.simplemachines.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** Item and fluid moving hooks, filled in by each loader with its own transfer API. Fluid amounts are in mB. */
public final class Transfer {
    private Transfer() {
    }

    public interface Lookup {
        boolean has(Level level, BlockPos pos, Direction side);
    }

    public interface Mover<T> {
        /** Moves up to max of whatever passes the filter out of from (seen from fromSide) into to, reporting what moved. */
        int move(ServerLevel level, BlockPos from, Direction fromSide, BlockPos to, Direction toSide, int max,
                 Predicate<T> filter, Consumer<T> moved);
    }

    public interface Probe<T> {
        /** Whether the block at pos would take at least some of this right now. */
        boolean accepts(ServerLevel level, BlockPos pos, Direction side, T what);
    }

    public interface ItemFluid {
        @Nullable Fluid fluid(ItemStack stack);
    }

    public interface Invalidator {
        void invalidate(ServerLevel level, BlockPos pos);
    }

    public static Lookup lookup = (level, pos, side) -> false;
    public static Mover<ItemStack> mover = (level, from, fromSide, to, toSide, max, filter, moved) -> 0;
    public static Probe<ItemStack> probe = (level, pos, side, what) -> false;
    public static Lookup fluidLookup = (level, pos, side) -> false;
    public static Mover<Fluid> fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> 0;
    public static Probe<Fluid> fluidProbe = (level, pos, side, what) -> false;
    public static ItemFluid itemFluid = stack -> null;
    public static Invalidator invalidator = (level, pos) -> {};
}
