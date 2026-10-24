package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class BufferEnergyHandler extends SnapshotJournal<Integer> implements EnergyHandler {
    private final EnergyBuffer buffer;
    private final boolean in;
    private final boolean out;

    public BufferEnergyHandler(EnergyBuffer buffer) {
        this(buffer, true, true);
    }

    public BufferEnergyHandler(EnergyBuffer buffer, boolean in, boolean out) {
        this.buffer = buffer;
        this.in = in;
        this.out = out;
    }

    @Override
    public long getAmountAsLong() {
        return buffer.energy;
    }

    @Override
    public long getCapacityAsLong() {
        return buffer.capacity;
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        if (!in) return 0;
        final int n = Math.min(buffer.capacity - buffer.energy, Math.min(amount, buffer.maxIn));
        if (n <= 0) return 0;
        updateSnapshots(tx);
        buffer.energy += n;
        return n;
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        if (!out) return 0;
        final int n = Math.min(buffer.energy, Math.min(amount, buffer.maxOut));
        if (n <= 0) return 0;
        updateSnapshots(tx);
        buffer.energy -= n;
        return n;
    }

    @Override
    protected Integer createSnapshot() {
        return buffer.energy;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        buffer.energy = snapshot;
    }

    @Override
    protected void onRootCommit(Integer original) {
        if (buffer.energy != original) buffer.changed();
    }
}
