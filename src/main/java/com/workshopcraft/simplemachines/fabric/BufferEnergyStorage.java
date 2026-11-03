package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

public class BufferEnergyStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
    private final EnergyBuffer buffer;
    private final boolean in;
    private final boolean out;

    public BufferEnergyStorage(EnergyBuffer buffer) {
        this(buffer, true, true);
    }

    public BufferEnergyStorage(EnergyBuffer buffer, boolean in, boolean out) {
        this.buffer = buffer;
        this.in = in;
        this.out = out;
    }

    @Override
    public boolean supportsInsertion() {
        return in && buffer.maxIn > 0;
    }

    @Override
    public boolean supportsExtraction() {
        return out && buffer.maxOut > 0;
    }

    @Override
    public long insert(long amount, TransactionContext tx) {
        if (!in) return 0;
        final int n = (int) Math.min(buffer.capacity - buffer.energy, Math.min(amount, buffer.maxIn));
        if (n <= 0) return 0;
        updateSnapshots(tx);
        buffer.energy += n;
        return n;
    }

    @Override
    public long extract(long amount, TransactionContext tx) {
        if (!out) return 0;
        final int n = (int) Math.min(buffer.energy, Math.min(amount, buffer.maxOut));
        if (n <= 0) return 0;
        updateSnapshots(tx);
        buffer.energy -= n;
        return n;
    }

    @Override
    public long getAmount() {
        return buffer.energy;
    }

    @Override
    public long getCapacity() {
        return buffer.capacity;
    }

    @Override
    protected Integer createSnapshot() {
        return buffer.energy;
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
        buffer.energy = snapshot;
    }

    @Override
    protected void onFinalCommit() {
        buffer.changed();
    }
}
