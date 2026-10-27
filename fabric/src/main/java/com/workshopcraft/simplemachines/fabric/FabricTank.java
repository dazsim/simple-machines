package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.pipe.FluidStore;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.level.material.Fluid;

/** A tank or machine fluid slot as one storage slot in droplets. */
final class FabricTank extends SnapshotParticipant<FabricTank.Snap> implements SingleSlotStorage<FluidVariant> {
    record Snap(Fluid fluid, long amount) {
    }

    private final FluidStore tank;

    FabricTank(FluidStore tank) {
        this.tank = tank;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || !resource.componentsMatch(net.minecraft.core.component.DataComponentPatch.EMPTY) || maxAmount <= 0) return 0;
        final long n = Math.min(maxAmount, tank.room(resource.getFluid()));
        if (n <= 0) return 0;
        updateSnapshots(transaction);
        tank.set(resource.getFluid(), tank.amount() + n);
        return n;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (!tank.drainable() || resource.isBlank() || maxAmount <= 0 || tank.amount() == 0) return 0;
        if (TankBlockEntity.still(resource.getFluid()) != tank.fluid() || !resource.componentsMatch(net.minecraft.core.component.DataComponentPatch.EMPTY)) return 0;
        final long n = Math.min(maxAmount, tank.amount());
        updateSnapshots(transaction);
        tank.set(tank.fluid(), tank.amount() - n);
        return n;
    }

    @Override
    public boolean supportsExtraction() {
        return tank.drainable();
    }

    @Override
    public boolean isResourceBlank() {
        return tank.amount() == 0;
    }

    @Override
    public FluidVariant getResource() {
        return tank.amount() == 0 ? FluidVariant.blank() : FluidVariant.of(tank.fluid());
    }

    @Override
    public long getAmount() {
        return tank.amount();
    }

    @Override
    public long getCapacity() {
        return tank.capacity();
    }

    @Override
    protected Snap createSnapshot() {
        return new Snap(tank.fluid(), tank.amount());
    }

    @Override
    protected void readSnapshot(Snap snapshot) {
        tank.set(snapshot.fluid(), snapshot.amount());
    }

    @Override
    protected void onFinalCommit() {
        tank.changed();
    }
}
