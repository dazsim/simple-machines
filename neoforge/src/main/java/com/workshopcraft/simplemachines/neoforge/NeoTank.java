package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** The tank as one slot in mB. */
final class NeoTank extends SnapshotJournal<NeoTank.Snap> implements ResourceHandler<FluidResource> {
    record Snap(Fluid fluid, long amount) {
    }

    private static final long MB = TankBlockEntity.MB;
    private final TankBlockEntity tank;

    NeoTank(TankBlockEntity tank) {
        this.tank = tank;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return index == 0 && tank.amount() > 0 ? FluidResource.of(tank.fluid()) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return index == 0 ? tank.amount() / MB : 0;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return index == 0 ? TankBlockEntity.CAPACITY / MB : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return index == 0 && !resource.isEmpty() && resource.isComponentsPatchEmpty() && resource.getFluid() != Fluids.EMPTY;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || !isValid(index, resource)) return 0;
        final int n = (int) Math.min(amount, tank.room(resource.getFluid()) / MB);
        if (n <= 0) return 0;
        updateSnapshots(transaction);
        tank.set(resource.getFluid(), tank.amount() + n * MB);
        return n;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || index != 0 || resource.isEmpty() || !resource.isComponentsPatchEmpty()) return 0;
        if (tank.amount() == 0 || TankBlockEntity.still(resource.getFluid()) != tank.fluid()) return 0;
        final int n = (int) Math.min(amount, tank.amount() / MB);
        if (n <= 0) return 0;
        updateSnapshots(transaction);
        tank.set(tank.fluid(), tank.amount() - n * MB);
        return n;
    }

    @Override
    protected Snap createSnapshot() {
        return new Snap(tank.fluid(), tank.amount());
    }

    @Override
    protected void revertToSnapshot(Snap snapshot) {
        tank.set(snapshot.fluid(), snapshot.amount());
    }

    @Override
    protected void onRootCommit(Snap originalState) {
        tank.changed();
    }
}
