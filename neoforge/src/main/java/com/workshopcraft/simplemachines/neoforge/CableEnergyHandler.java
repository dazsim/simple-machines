package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.CableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/** Passes energy offered to a cable on to the machines at the ends of its network. */
public class CableEnergyHandler extends SnapshotJournal<Integer> implements EnergyHandler {
    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction from;
    private CableNetwork net;

    public CableEnergyHandler(ServerLevel level, BlockPos pos, @Nullable Direction from) {
        this.level = level;
        this.pos = pos.immutable();
        this.from = from;
    }

    @Override
    public long getAmountAsLong() {
        return 0;
    }

    @Override
    public long getCapacityAsLong() {
        return CableBlock.RATE;
    }

    @Override
    public int insert(int amount, TransactionContext tx) {
        net = CableNetwork.of(level, pos);
        int left = Math.min(amount, net.budget(level.getGameTime()));
        if (left <= 0 || net.ends.isEmpty()) return 0;
        final BlockPos source = from == null ? null : pos.relative(from);
        int moved = 0;
        final int size = net.ends.size();
        for (int i = 0; i < size && left > 0; i++) {
            final CableNetwork.End end = net.ends.get((net.next + i) % size);
            if (end.pos().equals(source)) continue;
            final EnergyHandler h = level.getCapability(Capabilities.Energy.BLOCK, end.pos(), end.side());
            if (h == null) continue;
            final int n = h.insert(left, tx);
            moved += n;
            left -= n;
        }
        net.next = (net.next + 1) % size;
        if (moved > 0) {
            updateSnapshots(tx);
            net.moved += moved;
        }
        return moved;
    }

    @Override
    public int extract(int amount, TransactionContext tx) {
        return 0;
    }

    @Override
    protected Integer createSnapshot() {
        return net.moved;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        net.moved = snapshot;
    }
}
