package com.workshopcraft.simplemachines.machine;

import net.minecraft.core.Direction;

/** A machine whose sides can be set to push their output out. */
public interface Ported {
    /** Bit per Direction#get3DDataValue for each side set to output. */
    int outputSides();

    static int bit(Direction d) {
        return 1 << d.get3DDataValue();
    }
}
