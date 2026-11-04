package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class BufferEnergyHandler implements IEnergyStorage {
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
    public int receiveEnergy(int amount, boolean simulate) {
        if (!in) return 0;
        final int n = Math.min(buffer.capacity - buffer.energy, Math.min(amount, buffer.maxIn));
        if (n <= 0) return 0;
        if (!simulate) {
            buffer.energy += n;
            buffer.changed();
        }
        return n;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        if (!out) return 0;
        final int n = Math.min(buffer.energy, Math.min(amount, buffer.maxOut));
        if (n <= 0) return 0;
        if (!simulate) {
            buffer.energy -= n;
            buffer.changed();
        }
        return n;
    }

    @Override
    public int getEnergyStored() {
        return buffer.energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return buffer.capacity;
    }

    @Override
    public boolean canExtract() {
        return out;
    }

    @Override
    public boolean canReceive() {
        return in;
    }
}
