package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/** Chunk loader tickets that load and tick like a forced chunk, but are our own type so /forceload remove leaves them alone. */
public final class ChunkTickets {
    private static final TicketType<ChunkPos> TYPE = TicketType.create("simplemachines:chunk_loader", Comparator.comparingLong(ChunkPos::toLong));

    private ChunkTickets() {}

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos);
    }
}
