package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.CableNetwork;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.ender.EnderAccess;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import team.reborn.energy.api.EnergyStorage;

/** Passes energy offered to a cable on to the machines at the ends of its network. */
public class CableEnergyStorage extends SnapshotParticipant<Integer> implements EnergyStorage {
    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction from;
    private CableNetwork net;
    private final Set<BlockPos> blow = new LinkedHashSet<>();

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
        if (!CableBlock.open(level, pos, from)) return 0;
        net = CableNetwork.of(level, pos);
        long left = Math.min(amount, net.budget(level.getGameTime()));
        if (left <= 0 || net.ends.isEmpty()) return 0;
        final BlockPos source = from == null ? null : pos.relative(from);
        long moved = 0;
        final int size = net.ends.size();
        for (int i = 0; i < size && left > 0; i++) {
            final CableNetwork.End end = net.ends.get((net.next + i) % size);
            if (end.pos().equals(source)) continue;
            final int room = (int) Math.min(left, Integer.MAX_VALUE);
            final long n = EnderAccess.through(level, end.pos(), end.pos().relative(end.side()), () -> offer(end, room, tx));
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

    /** Offers energy to one end, with the cable touching it speaking for the network. */
    private int offer(CableNetwork.End end, int amount, TransactionContext tx) {
        if (Voltage.overloads(level, end.pos(), end.side(), net.tier)) {
            updateSnapshots(tx);
            blow.add(end.pos());
            return 0;
        }
        final EnergyStorage s = EnergyStorage.SIDED.find(level, end.pos(), end.side());
        if (s == null || !s.supportsInsertion()) return 0;
        return (int) s.insert(amount, tx);
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
        return level.getBlockState(pos).getBlock() instanceof CableBlock c ? c.rate() : 0;
    }

    @Override
    protected Integer createSnapshot() {
        return net.moved;
    }

    @Override
    protected void readSnapshot(Integer snapshot) {
        net.moved = snapshot;
        blow.clear();
    }

    @Override
    protected void onFinalCommit() {
        for (BlockPos p : blow) Voltage.blow(level, p);
        blow.clear();
    }
}
