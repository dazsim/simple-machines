package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.CableNetwork;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/** Passes energy offered to a cable on to the machines at the ends of its network. */
public class CableEnergyStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction from;
    private CableNetwork net;

    public CableEnergyStorage(ServerLevel level, BlockPos pos, @Nullable Direction from) {
        this.level = level;
        this.pos = pos.immutable();
        this.from = from;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

    @Override
    public long insert(long amount, TransactionContext tx) {
        net = CableNetwork.of(level, pos);
        long left = Math.min(amount, net.budget(level.getGameTime()));
        if (left <= 0 || net.ends.isEmpty()) return 0;
        final BlockPos source = from == null ? null : pos.relative(from);
        long moved = 0;
        final int size = net.ends.size();
        for (int i = 0; i < size && left > 0; i++) {
            final CableNetwork.End end = net.ends.get((net.next + i) % size);
            if (end.pos().equals(source)) continue;
            final EnergyStorage s = EnergyStorage.SIDED.find(level, end.pos(), end.side());
            if (s == null || !s.supportsInsertion()) continue;
            final long n = s.insert(left, tx);
            moved += n;
            left -= n;
        }
        net.next = (net.next + 1) % size;
        if (moved > 0) {
            updateSnapshots(tx);
            net.moved += (int) moved;
        }
        return moved;
    }

    @Override
    public long extract(long amount, TransactionContext tx) {
        return 0;
    }

    @Override
    public long getAmount() {
        return 0;
    }

    @Override
    public long getCapacity() {
        return CableBlock.RATE;
    }

    @Override
    protected Integer createSnapshot() {
        return net.moved;
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
        net.moved = snapshot;
    }
}
