package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import com.workshopcraft.simplemachines.ender.EnderAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** A connected run of pipes and the blocks it delivers to. Filtered ends that want something get it first, round-robin. */
public final class PipeNetwork {
    private static final int MAX_SIZE = 4096;
    private static final Map<ServerLevel, Map<BlockPos, PipeNetwork>> CACHE = new WeakHashMap<>();

    /** The block at pos, seen from side, reached through the end of the pipe at pipe facing dir. */
    public record Target(BlockPos pos, Direction side, BlockPos pipe, Direction dir) {
    }

    public final List<Target> targets = new ArrayList<>();
    private final Set<BlockPos> pipes = new HashSet<>();
    private int next, nextAllow;

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
                    net.targets.add(new Target(n, d.getOpposite(), pos, d));
                }
            }
        }
        for (BlockPos p : net.pipes) map.put(p, net);
        return net;
    }

    public Set<BlockPos> pipes() {
        return pipes;
    }

    /** Moves up to one batch of items out of the inventory at from. */
    public int pull(ServerLevel level, BlockPos from, Direction fromSide) {
        return route(level, from, fromSide, ItemPipeBlock.BATCH, Transfer.mover, Transfer.probe, PipeFilter::matches, s -> {});
    }

    /** Moves up to max mB out of the fluid store at from, reporting each fluid that moved. */
    public int pullFluid(ServerLevel level, BlockPos from, Direction fromSide, int max, Consumer<Fluid> moved) {
        return route(level, from, fromSide, max, Transfer.fluidMover, Transfer.fluidProbe, PipeFilter::matchesFluid, moved);
    }

    private static PipeFilter filterAt(ServerLevel level, Target t) {
        return level.getBlockEntity(t.pipe()) instanceof ConduitBlockEntity be ? be.filter(t.dir()) : null;
    }

    /** The extract end's pipe speaks for the source, each delivering end's pipe for its target. */
    private <T> int route(ServerLevel level, BlockPos from, Direction fromSide, int max, Transfer.Mover<T> mover,
                          Transfer.Probe<T> probe, BiPredicate<PipeFilter, T> match, Consumer<T> moved) {
        return EnderAccess.through(level, from, from.relative(fromSide), () -> routeAll(level, from, fromSide, max, mover, probe, match, moved));
    }

    private <T> int routeAll(ServerLevel level, BlockPos from, Direction fromSide, int max, Transfer.Mover<T> mover,
                          Transfer.Probe<T> probe, BiPredicate<PipeFilter, T> match, Consumer<T> moved) {
        final List<Target> allow = new ArrayList<>(), rest = new ArrayList<>();
        final Map<Target, PipeFilter> filters = new HashMap<>();
        for (Target t : targets) {
            if (t.pos().equals(from)) continue;
            final PipeFilter f = filterAt(level, t);
            if (f != null) filters.put(t, f);
            (f == null || f.deny ? rest : allow).add(t);
        }
        int total = 0;
        for (int i = 0; i < allow.size(); i++) {
            final Target t = allow.get(Math.floorMod(nextAllow++, allow.size()));
            final PipeFilter f = filters.get(t);
            final int n = EnderAccess.through(level, t.pos(), t.pipe(),
                    () -> mover.move(level, from, fromSide, t.pos(), t.side(), max, x -> match.test(f, x), moved));
            if (n > 0) {
                total += n;
                break;
            }
        }
        final Predicate<T> unclaimed = x -> {
            for (Target a : allow) {
                if (match.test(filters.get(a), x) && EnderAccess.check(level, a.pos(), a.pipe(), () -> probe.accepts(level, a.pos(), a.side(), x))) return false;
            }
            return true;
        };
        if (total >= max) return total;
        final int left = max - total;
        for (int i = 0; i < rest.size(); i++) {
            final Target t = rest.get(Math.floorMod(next++, rest.size()));
            final PipeFilter f = filters.get(t);
            final int n = EnderAccess.through(level, t.pos(), t.pipe(), () -> mover.move(level, from, fromSide, t.pos(), t.side(), left,
                    x -> (f == null || !match.test(f, x)) && unclaimed.test(x), moved));
            if (n > 0) {
                total += n;
                break;
            }
        }
        return total;
    }
}
