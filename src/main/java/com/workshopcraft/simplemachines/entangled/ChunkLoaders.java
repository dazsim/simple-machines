package com.workshopcraft.simplemachines.entangled;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.ServerConfig;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=26.1
import net.minecraft.server.level.TicketType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every running Chunk Loader on the server. Tickets are not saved by the game; this list is, and puts them back on start.
 * A loader covers the (2r+1) square of chunks around its own with one ticket at entity ticking level.
 */
public class ChunkLoaders extends SavedData {
    /** Loads and ticks like a forced chunk, without timing out, but is not written to the chunk ticket file. */
    //? if >=26.1 {
    public static final TicketType TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    //?} else {
    /*
    *///?}
    private static final int SWEEP = 100;

    public record Entry(GlobalPos pos, UUID owner, int radius) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(Entry::owner),
                Codec.INT.fieldOf("radius").forGetter(Entry::radius)
        ).apply(i, Entry::new));

        public int chunks() {
            return side(radius) * side(radius);
        }

        ChunkPos chunk() {
            return ChunkPos.containing(pos.pos());
        }

        boolean sameTicket(Entry o) {
            return o.pos.dimension().equals(pos.dimension()) && o.chunk().equals(chunk()) && o.radius == radius;
        }
    }

    public static final Codec<ChunkLoaders> CODEC = Entry.CODEC.listOf().xmap(ChunkLoaders::new, d -> List.copyOf(d.entries.values()));
    public static final SavedDataType<ChunkLoaders> TYPE = new SavedDataType<>(SimpleMachines.id("chunk_loaders"), ChunkLoaders::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<GlobalPos, Entry> entries = new LinkedHashMap<>();
    /** Entries whose ticket is in the level right now. Not saved: tickets are rebuilt on start. */
    private final Set<GlobalPos> ticketed = new HashSet<>();

    public ChunkLoaders() {
    }

    private ChunkLoaders(List<Entry> list) {
        for (Entry e : list) entries.put(e.pos(), e);
    }

    public static ChunkLoaders get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static int side(int radius) {
        return 2 * radius + 1;
    }

    public int used(UUID owner) {
        int n = 0;
        for (Entry e : entries.values()) if (e.owner().equals(owner)) n += e.chunks();
        return n;
    }

    public boolean has(GlobalPos pos) {
        return entries.containsKey(pos);
    }

    /** Whether the loader at pos holds its chunks right now, rather than waiting for its owner to come back. */
    public boolean holding(GlobalPos pos) {
        return ticketed.contains(pos);
    }

    /** Starts or resizes a loader. False when it would take the owner past the cap; nothing changes then. */
    public boolean start(MinecraftServer server, GlobalPos pos, UUID owner, int radius) {
        final Entry old = entries.get(pos);
        final Entry next = new Entry(pos, owner, radius);
        if (next.equals(old)) {
            sync(server, next);
            return true;
        }
        final int cap = ServerConfig.integer(server, ServerConfig.MAX_CHUNKS);
        if (cap > 0 && used(owner) - (old == null || !old.owner().equals(owner) ? 0 : old.chunks()) + next.chunks() > cap) return false;
        if (old != null) stop(server, pos);
        entries.put(pos, next);
        setDirty();
        sync(server, next);
        return true;
    }

    public void stop(MinecraftServer server, GlobalPos pos) {
        final Entry e = entries.remove(pos);
        if (e == null) return;
        setDirty();
        release(server, e);
    }

    private boolean wanted(MinecraftServer server, Entry e) {
        return ServerConfig.bool(server, ServerConfig.CHUNK_LOADING)
                && (ServerConfig.bool(server, ServerConfig.LOAD_OFFLINE) || server.getPlayerList().getPlayer(e.owner()) != null);
    }

    /** Puts the ticket in or takes it out to match the config and whether the owner is online. */
    private void sync(MinecraftServer server, Entry e) {
        if (wanted(server, e)) hold(server, e);
        else release(server, e);
    }

    private void hold(MinecraftServer server, Entry e) {
        if (!ticketed.add(e.pos())) return;
        final ServerLevel level = server.getLevel(e.pos().dimension());
        //? if >=26.1 {
        if (level != null) level.getChunkSource().addTicketWithRadius(TICKET, e.chunk(), e.radius() + 2);
        //?} else {
        /*if (level != null) com.workshopcraft.simplemachines.compat.mc.ChunkTickets.add(level, e.chunk(), e.radius() + 2);
        *///?}
    }

    private void release(MinecraftServer server, Entry e) {
        if (!ticketed.remove(e.pos())) return;
        // two loaders in one chunk at one size share a single ticket, so only the last one out takes it
        for (GlobalPos p : ticketed) if (entries.get(p) instanceof Entry o && o.sameTicket(e)) return;
        final ServerLevel level = server.getLevel(e.pos().dimension());
        //? if >=26.1 {
        if (level != null) level.getChunkSource().removeTicketWithRadius(TICKET, e.chunk(), e.radius() + 2);
        //?} else {
        /*if (level != null) com.workshopcraft.simplemachines.compat.mc.ChunkTickets.remove(level, e.chunk(), e.radius() + 2);
        *///?}
    }

    public void restore(MinecraftServer server) {
        ticketed.clear();
        for (Entry e : List.copyOf(entries.values())) sync(server, e);
    }

    public void resync(MinecraftServer server, GlobalPos pos) {
        if (entries.get(pos) instanceof Entry e) sync(server, e);
    }

    public void resync(MinecraftServer server) {
        for (Entry e : List.copyOf(entries.values())) sync(server, e);
    }

    /** Drops loaders whose device is gone, and follows config edits. */
    public void tick(MinecraftServer server) {
        if (server.getTickCount() % SWEEP != 0 || entries.isEmpty()) return;
        final List<GlobalPos> gone = new ArrayList<>();
        for (Entry e : entries.values()) {
            final ServerLevel level = server.getLevel(e.pos().dimension());
            if (level == null) gone.add(e.pos());
            else if (level.isLoaded(e.pos().pos()) && !(level.getBlockEntity(e.pos().pos()) instanceof EntangledDeviceBlockEntity d && d.loading())) gone.add(e.pos());
        }
        for (GlobalPos p : gone) stop(server, p);
        resync(server);
    }

    public static void onServerStarted(MinecraftServer server) {
        ServerConfig.init(server);
        get(server).restore(server);
    }

    public static void onJoin(ServerPlayer player) {
        get(player.level().getServer()).resync(player.level().getServer());
    }

    public static void onLeave(ServerPlayer player) {
        final MinecraftServer server = player.level().getServer();
        final ChunkLoaders data = get(server);
        if (ServerConfig.bool(server, ServerConfig.LOAD_OFFLINE)) return;
        for (Entry e : List.copyOf(data.entries.values())) if (e.owner().equals(player.getUUID())) data.release(server, e);
    }

    public List<Entry> entries() {
        return List.copyOf(entries.values());
    }
}
