package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.pipe.FluidStore;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** The tank as one slot in mB. */
final class NeoTank implements IFluidHandler {
    private static final long MB = TankBlockEntity.MB;
    private final FluidStore tank;

    NeoTank(FluidStore tank) {
        this.tank = tank;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return index == 0 && tank.amount() > 0 ? new FluidStack(tank.fluid(), (int) (tank.amount() / MB)) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int index) {
        return index == 0 ? (int) (tank.capacity() / MB) : 0;
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return index == 0 && !stack.isEmpty() && stack.isComponentsPatchEmpty() && stack.getFluid() != Fluids.EMPTY;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        final int n = (int) Math.min(resource.getAmount(), tank.room(resource.getFluid()) / MB);
        if (n <= 0) return 0;
        if (action.execute()) {
            tank.set(resource.getFluid(), tank.amount() + n * MB);
            tank.changed();
        }
        return n;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.isComponentsPatchEmpty()) return FluidStack.EMPTY;
        if (tank.amount() == 0 || TankBlockEntity.still(resource.getFluid()) != tank.fluid()) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        if (amount <= 0 || !tank.drainable() || tank.amount() == 0) return FluidStack.EMPTY;
        final int n = (int) Math.min(amount, tank.amount() / MB);
        if (n <= 0) return FluidStack.EMPTY;
        final FluidStack out = new FluidStack(tank.fluid(), n);
        if (action.execute()) {
            tank.set(tank.fluid(), tank.amount() - n * MB);
            tank.changed();
        }
        return out;
    }
}
