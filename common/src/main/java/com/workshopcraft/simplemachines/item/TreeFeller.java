package com.workshopcraft.simplemachines.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The rest of a natural tree above a cut log, found the way Simple Golems lumberjacks do: same log kind, joined
 * sideways, diagonally or upwards, within reach of the cut. A tree needs natural leaves touching its logs, so log
 * cabins and stripped builds are left alone.
 */
public final class TreeFeller {
    public static final int MAX_LOGS = 256;
    public static final int MAX_REACH = 12;

    private TreeFeller() {
    }

    /** Logs to fell after the one at cut was broken, highest first; empty when it is not a natural tree. */
    public static List<BlockPos> logs(ServerLevel level, BlockPos cut, BlockState cutState) {
        final Block kind = cutState.getBlock();
        final Set<BlockPos> logs = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(cut.immutable());
        boolean leafy = false;
        while (!queue.isEmpty()) {
            final BlockPos p = queue.poll();
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, 0, -1), p.offset(1, 1, 1))) {
                if (n.equals(cut) || logs.contains(n) || Math.abs(n.getX() - cut.getX()) > MAX_REACH || Math.abs(n.getZ() - cut.getZ()) > MAX_REACH) continue;
                if (!level.getBlockState(n).is(kind)) continue;
                final BlockPos imm = n.immutable();
                logs.add(imm);
                queue.add(imm);
                if (logs.size() > MAX_LOGS) return List.of();
            }
        }
        for (BlockPos log : logs) {
            for (Direction d : Direction.values()) {
                if (naturalLeaf(level.getBlockState(log.relative(d)))) {
                    leafy = true;
                    break;
                }
            }
            if (leafy) break;
        }
        if (!leafy) return List.of();
        final List<BlockPos> sorted = new ArrayList<>(logs);
        sorted.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()));
        return sorted;
    }

    private static boolean naturalLeaf(BlockState s) {
        return s.getBlock() instanceof LeavesBlock && s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT)
                && s.getValue(LeavesBlock.DISTANCE) == 1;
    }
}
