package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Client tick: a looping motor sound and sparks, dust or chips at the block face while a charged tool digs. */
public final class ToolEffects {
    private static Loop loop;
    private static int age;

    private ToolEffects() {
    }

    public static void tick(Minecraft mc) {
        final PoweredToolItem tool = digging(mc);
        if (tool == null) return;
        if (loop == null || loop.isStopped() || loop.tool != tool || !mc.getSoundManager().isActive(loop)) {
            loop = new Loop(tool);
            mc.getSoundManager().play(loop);
        }
        if (++age % 2 == 0 && mc.hitResult instanceof BlockHitResult hit) particles(mc, tool, hit);
    }

    /** The tool being used on a block right now, or null. */
    static PoweredToolItem digging(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || !mc.gameMode.isDestroying()) return null;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        final ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof PoweredToolItem tool) || !PoweredToolItem.powered(stack)) return null;
        final BlockState state = mc.level.getBlockState(hit.getBlockPos());
        return tool.getDestroySpeed(stack, state) > 1.0F || tool.refuses(stack, state) ? tool : null;
    }

    private static void particles(Minecraft mc, PoweredToolItem tool, BlockHitResult hit) {
        final BlockPos pos = hit.getBlockPos();
        final BlockState state = mc.level.getBlockState(pos);
        final Direction face = hit.getDirection();
        final RandomSource r = mc.level.getRandom();
        final Vec3 at = hit.getLocation().add(face.getStepX() * 0.05, face.getStepY() * 0.05, face.getStepZ() * 0.05);
        final boolean refused = tool.refuses(mc.player.getMainHandItem(), state);
        final boolean hard = refused || state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        final int n = refused ? 22 : hard ? 16 : 4;
        for (int i = 0; i < n; i++) {
            final double sx = face.getStepX() * 0.15 + (r.nextDouble() - 0.5) * 0.25;
            final double sy = face.getStepY() * 0.15 + r.nextDouble() * 0.15;
            final double sz = face.getStepZ() * 0.15 + (r.nextDouble() - 0.5) * 0.25;
            final double boost = 1.6 + r.nextDouble() * 1.4;
            if (hard) mc.level.addParticle(refused ? SimpleMachines.WHITE_SPARK : SimpleMachines.HOT_SPARK, at.x, at.y, at.z,
                    sx * boost, sy * 1.3 + 0.05, sz * boost);
            else mc.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, sx, sy, sz);
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final PoweredToolItem tool;
        private int idle;

        Loop(PoweredToolItem tool) {
            super(tool.sound, SoundSource.PLAYERS, RandomSource.create());
            this.tool = tool;
            looping = true;
            volume = 0.6F;
            follow();
        }

        private void follow() {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            x = mc.player.getX();
            y = mc.player.getEyeY();
            z = mc.player.getZ();
        }

        @Override
        public void tick() {
            final Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.isRemoved()) {
                stop();
                return;
            }
            follow();
            if (digging(mc) == tool) idle = 0;
            else if (++idle > 2) stop();
        }
    }
}
