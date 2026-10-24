package com.workshopcraft.simplemachines.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** A connected run of item pipes and the inventories it delivers to, handed out round-robin. */
public final class PipeNetwork {
    private static final int MAX_SIZE = 4096;
    private static final Map<ServerLevel, Map<BlockPos, PipeNetwork>> CACHE = new WeakHashMap<>();

    public record Target(BlockPos pos, Direction side) {
    }

    public final List<Target> targets = new ArrayList<>();
    private final Set<BlockPos> pipes = new HashSet<>();
    private int next;

    public static void invalidate(ServerLevel level) {
        CACHE.remove(level);
    }

    public static PipeNetwork of(ServerLevel level, BlockPos start) {
        final Map<BlockPos, PipeNetwork> map = CACHE.computeIfAbsent(level, l -> new HashMap<>());
        final PipeNetwork known = map.get(start);
        if (known != null) return known;
        final PipeNetwork net = new PipeNetwork();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        net.pipes.add(start.immutable());
        while (!queue.isEmpty() && net.pipes.size() < MAX_SIZE) {
            final BlockPos pos = queue.poll();
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ItemPipeBlock)) continue;
            for (Direction d : Direction.values()) {
                final Link link = state.getValue(ItemPipeBlock.LINKS.get(d));
                final BlockPos n = pos.relative(d);
                if (link == Link.PIPE) {
                    if (net.pipes.add(n)) queue.add(n);
                } else if (link == Link.LINK) {
                    net.targets.add(new Target(n, d.getOpposite()));
                }
            }
        }
        for (BlockPos p : net.pipes) map.put(p, net);
        return net;
    }

    /** Moves one batch out of the inventory at from into the next target that takes it. */
    public int pull(ServerLevel level, BlockPos from, Direction fromSide) {
        final int n = targets.size();
        for (int i = 0; i < n; i++) {
            final Target t = targets.get(Math.floorMod(next++, n));
            if (t.pos().equals(from)) continue;
            final int moved = Transfer.mover.move(level, from, fromSide, t.pos(), t.side(), ItemPipeBlock.BATCH);
            if (moved > 0) return moved;
        }
        return 0;
    }
}
