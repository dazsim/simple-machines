package com.workshopcraft.simplemachines.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface Ticking {
    void serverTick(ServerLevel level, BlockPos pos, BlockState state);
}
