package com.workshopcraft.simplemachines.cable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A connected run of cables. Energy offered to any cable goes straight to the machines on the ends,
 * at most {@link CableBlock#RATE} FE per tick for the whole run.
 */
public final class CableNetwork {
    private static final int MAX_SIZE = 4096;
    private static final Map<ServerLevel, Map<BlockPos, CableNetwork>> CACHE = new WeakHashMap<>();

    public record End(BlockPos pos, Direction side) {
    }

    public final List<End> ends = new ArrayList<>();
    private final Set<BlockPos> cables = new HashSet<>();
    public int next;
    public long tick = -1;
    public int moved;

    public static void invalidate(ServerLevel level) {
        CACHE.remove(level);
    }

    public static CableNetwork of(ServerLevel level, BlockPos start) {
        final Map<BlockPos, CableNetwork> map = CACHE.computeIfAbsent(level, l -> new HashMap<>());
        final CableNetwork known = map.get(start);
        if (known != null) return known;
        final CableNetwork net = new CableNetwork();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        net.cables.add(start.immutable());
        while (!queue.isEmpty() && net.cables.size() < MAX_SIZE) {
            final BlockPos pos = queue.poll();
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof CableBlock)) continue;
            for (Direction d : Direction.values()) {
                if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d))) continue;
                final BlockPos n = pos.relative(d);
                if (level.getBlockState(n).getBlock() instanceof CableBlock) {
                    if (net.cables.add(n)) queue.add(n);
                } else {
                    net.ends.add(new End(n, d.getOpposite()));
                }
            }
        }
        for (BlockPos p : net.cables) map.put(p, net);
        return net;
    }

    /** Room left in this tick's budget. */
    public int budget(long gameTime) {
        if (tick != gameTime) {
            tick = gameTime;
            moved = 0;
        }
        return CableBlock.RATE - moved;
    }

    public boolean isCable(BlockPos pos) {
        return cables.contains(pos);
    }
}
