package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.CableNetwork;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.entangled.EntangledAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/** Passes energy offered to a cable on to the machines at the ends of its network. */
public class CableEnergyHandler implements IEnergyStorage {
    private final ServerLevel level;
    private final BlockPos pos;
    private final @Nullable Direction from;
    private CableNetwork net;
    private final Set<BlockPos> blow = new LinkedHashSet<>();

    public CableEnergyHandler(ServerLevel level, BlockPos pos, @Nullable Direction from) {
        this.level = level;
        this.pos = pos.immutable();
        this.from = from;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        if (!CableBlock.open(level, pos, from)) return 0;
        net = CableNetwork.of(level, pos);
        int left = Math.min(amount, net.budget(level.getGameTime()));
        if (left <= 0 || net.ends.isEmpty()) return 0;
        final BlockPos source = from == null ? null : pos.relative(from);
        int moved = 0;
        final int size = net.ends.size();
        for (int i = 0; i < size && left > 0; i++) {
            final CableNetwork.End end = net.ends.get((net.next + i) % size);
            if (end.pos().equals(source)) continue;
            final int room = left;
            final int n = EntangledAccess.through(level, end.pos(), end.pos().relative(end.side()), () -> offer(end, room, simulate));
            moved += n;
            left -= n;
        }
        if (simulate) {
            blow.clear();
            return moved;
        }
        net.next = (net.next + 1) % size;
        net.moved += moved;
        for (BlockPos p : blow) Voltage.blow(level, p);
        blow.clear();
        return moved;
    }

    /** Offers energy to one end, with the cable touching it speaking for the network. */
    private int offer(CableNetwork.End end, int amount, boolean simulate) {
        if (Voltage.overloads(level, end.pos(), end.side(), net.tier)) {
            blow.add(end.pos());
            return 0;
        }
        final IEnergyStorage h = level.getCapability(Capabilities.EnergyStorage.BLOCK, end.pos(), end.side());
        if (h == null) return 0;
        return h.receiveEnergy(amount, simulate);
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return level.getBlockState(pos).getBlock() instanceof CableBlock c ? c.rate() : 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
