package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/** Chunk loader tickets that load and tick like a forced chunk. */
public final class ChunkTickets {
    private static final TicketType<ChunkPos> TYPE = TicketType.FORCED;

    private ChunkTickets() {}

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos);
    }
}
