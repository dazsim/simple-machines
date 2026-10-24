package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** A machine that works a square in front of it (5x5 on iron, 9x9 on steel) and keeps what it gathers in nine output slots. */
public abstract class AreaBlockEntity extends WorkBlockEntity {
    protected AreaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxIn) {
        super(type, pos, state, 9, capacity, maxIn, 0);
    }

    public static int radius(Tier tier) {
        return tier == Tier.STEEL ? 4 : 2;
    }

    /** Centre of the worked square, at the machine's own height. */
    protected BlockPos centre(BlockState state, Tier tier) {
        final Direction facing = state.getValue(MachineBlock.FACING);
        return getBlockPos().relative(facing, radius(tier) + 1);
    }

    protected AABB area(BlockState state, Tier tier, int height) {
        final int r = radius(tier);
        final BlockPos c = centre(state, tier);
        return new AABB(c.getX() - r, c.getY(), c.getZ() - r, c.getX() + r + 1, c.getY() + height, c.getZ() + r + 1);
    }

    @Override
    protected boolean isOutput(int slot) {
        return true;
    }

    @Override
    public boolean energyOut(Direction side) {
        return false;
    }

    @Override
    public Layout layout() {
        return Layout.GRID;
    }

    /** Stores the stack, dropping anything that does not fit on top of the machine. */
    protected void keep(Level level, ItemStack stack) {
        final ItemStack left = store(stack);
        if (!left.isEmpty()) {
            final BlockPos p = getBlockPos();
            Containers.dropItemStack(level, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, left);
        }
    }
}
