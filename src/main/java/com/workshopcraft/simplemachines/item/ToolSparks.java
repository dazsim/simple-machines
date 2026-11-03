package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

/** Where digging sparks start and how they fly: off the face at the contact point, back toward and around the digger. */
public final class ToolSparks {
    public enum Kind { NONE, DUST, HOT, WHITE }

    private static Field destroying;
    private static boolean looked;

    private ToolSparks() {
    }

    public static Kind kind(PoweredToolItem tool, ItemStack stack, BlockState state) {
        if (!PoweredToolItem.powered(stack)) return Kind.NONE;
        final boolean refused = tool.refuses(stack, state);
        if (!refused && tool.getDestroySpeed(stack, state) <= 1.0F) return Kind.NONE;
        if (refused) return Kind.WHITE;
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? Kind.HOT : Kind.DUST;
    }

    public static int count(Kind kind, boolean remote) {
        return switch (kind) {
            case WHITE -> remote ? 14 : 22;
            case HOT -> remote ? 10 : 16;
            case DUST -> remote ? 3 : 4;
            case NONE -> 0;
        };
    }

    public static ParticleOptions particle(Kind kind, BlockState state) {
        return kind == Kind.WHITE ? SimpleMachines.WHITE_SPARK : kind == Kind.HOT ? SimpleMachines.HOT_SPARK
                : new BlockParticleOption(ParticleTypes.BLOCK, state);
    }

    /** The hit point on the face, a little out of it and toward the eye. */
    public static Vec3 contact(BlockHitResult hit, Vec3 eye) {
        final Direction face = hit.getDirection();
        final Vec3 at = hit.getLocation();
        final Vec3 back = eye.subtract(at).normalize();
        return at.add(face.getStepX() * 0.06 + back.x * 0.08, face.getStepY() * 0.06 + back.y * 0.08, face.getStepZ() * 0.06 + back.z * 0.08);
    }

    /** Spray off the face: the look ray reflected off it, plus a wide fan along the face and a little lift. */
    public static Vec3 spray(RandomSource r, Direction face, Vec3 look, Kind kind) {
        final Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        final Vec3 refl = look.subtract(n.scale(2 * look.dot(n)));
        final Vec3 t1 = face.getAxis() == Direction.Axis.Y ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        final Vec3 t2 = n.cross(t1);
        if (kind == Kind.DUST) return n.scale(0.15).add(t1.scale((r.nextDouble() - 0.5) * 0.25)).add(t2.scale((r.nextDouble() - 0.5) * 0.25));
        final double boost = 1.6 + r.nextDouble() * 1.4;
        return refl.scale(0.03 + r.nextDouble() * 0.05).add(n.scale(0.04))
                .add(t1.scale((r.nextDouble() - 0.5) * 0.36)).add(t2.scale((r.nextDouble() - 0.5) * 0.36))
                .scale(boost).add(0, 0.05 + r.nextDouble() * 0.06, 0);
    }

    /** Server side, every other tick while a player digs with a running tool: send the sparks to everyone else nearby. */
    public static void broadcast(PoweredToolItem tool, ItemStack stack, ServerLevel level, ServerPlayer player) {
        if (level.players().size() < 2 || !destroying(player.gameMode)) return;
        final HitResult pick = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(pick instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        final BlockState state = level.getBlockState(hit.getBlockPos());
        final Kind kind = kind(tool, stack, state);
        if (kind == Kind.NONE) return;
        final Vec3 at = contact(hit, player.getEyePosition());
        final Vec3 look = player.getLookAngle();
        final ParticleOptions particle = particle(kind, state);
        final RandomSource r = level.getRandom();
        for (ServerPlayer viewer : level.players()) {
            if (viewer == player || viewer.distanceToSqr(at) > 48 * 48) continue;
            for (int i = count(kind, true); i > 0; i--) {
                final Vec3 v = spray(r, hit.getDirection(), look, kind);
                level.sendParticles(viewer, particle, false, false, at.x, at.y, at.z, 0, v.x, v.y, v.z, 1.0);
            }
        }
    }

    /** Quarry drill biting into the top of a block, sunk this far into it. */
    public static void quarry(ServerLevel level, net.minecraft.core.BlockPos pos, BlockState state, float sunk) {
        final Kind kind = state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? Kind.HOT : Kind.DUST;
        final ParticleOptions particle = particle(kind, state);
        final double x = pos.getX() + 0.5, y = pos.getY() + 1.06 - sunk, z = pos.getZ() + 0.5;
        final Vec3 down = new Vec3(0, -1, 0);
        final RandomSource r = level.getRandom();
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(x, y, z) > 48 * 48) continue;
            for (int i = count(kind, true); i > 0; i--) {
                final Vec3 v = spray(r, Direction.UP, down, kind);
                level.sendParticles(viewer, particle, false, false, x, y, z, 0, v.x, v.y, v.z, 1.0);
            }
        }
    }

    private static boolean destroying(ServerPlayerGameMode mode) {
        if (!looked) {
            looked = true;
            try {
                destroying = ServerPlayerGameMode.class.getDeclaredField("isDestroyingBlock");
                destroying.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("No dig state for remote tool sparks", e);
            }
        }
        try {
            return destroying != null && destroying.getBoolean(mode);
        } catch (IllegalAccessException e) {
            return false;
        }
    }
}
