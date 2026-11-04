package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/** Chunk loader tickets that load and tick like a forced chunk. */
public final class ChunkTickets {
    //? if neoforge {
    private static final TicketType<ChunkPos> TYPE = TicketType.create("simplemachines:chunk_loader", Comparator.comparingLong(ChunkPos::toLong));
    //?} else
    /*private static final TicketType<ChunkPos> TYPE = TicketType.FORCED;*/

    private ChunkTickets() {}

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        //? if neoforge {
        level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos, true);
        //?} else
        /*level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos);*/
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        //? if neoforge {
        level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos, true);
        //?} else
        /*level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos);*/
    }
}
