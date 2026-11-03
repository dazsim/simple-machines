package com.workshopcraft.simplemachines.gen;

import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

final class Generators {
    private Generators() {
    }

    /** Offers the buffer to every side but skip at the given voltage. A lower rated block of ours on a side blows up instead. */
    static void pushOut(ServerLevel level, BlockPos pos, EnergyBuffer buf, int tier, @Nullable Direction skip) {
        int left = Math.min(buf.energy, buf.maxOut);
        for (Direction d : Direction.values()) {
            if (left <= 0) return;
            if (d == skip) continue;
            final BlockPos target = pos.relative(d);
            if (Voltage.overloads(level, target, d.getOpposite(), tier)) {
                if (EnergyBuffer.lookup.has(level, target, d.getOpposite())) {
                    Voltage.blow(level, target);
                    return;
                }
                continue;
            }
            final int n = EnergyBuffer.pusher.push(level, target, d.getOpposite(), left);
            if (n > 0) {
                left -= n;
                buf.energy -= n;
                buf.changed();
            }
        }
    }
}
