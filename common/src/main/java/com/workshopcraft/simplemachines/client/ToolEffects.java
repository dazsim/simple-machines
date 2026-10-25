package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.item.PoweredToolItem;
import com.workshopcraft.simplemachines.item.ToolSparks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/** Client tick: a looping motor sound and sparks off the bit tip (dust or chips at the block face) while a charged tool digs. */
public final class ToolEffects {
    // where the bit tip sits on screen in first person (x right, y up, -1..1) and how far out the sparks start
    private static final double TIP_X = 0.46, TIP_Y = -0.4, TIP_DEPTH = 0.75;
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
        final ItemStack stack = mc.player.getMainHandItem();
        final BlockState state = mc.level.getBlockState(hit.getBlockPos());
        final ToolSparks.Kind kind = ToolSparks.kind(tool, stack, state);
        if (kind == ToolSparks.Kind.NONE) return;
        final RandomSource r = mc.level.getRandom();
        final Direction face = hit.getDirection();
        final Vec3 look = mc.player.getLookAngle();
        final ParticleOptions particle = ToolSparks.particle(kind, state);
        final boolean tip = kind != ToolSparks.Kind.DUST && mc.options.getCameraType().isFirstPerson();
        final Vec3 at = tip ? tip(mc, hit) : ToolSparks.contact(hit, mc.player.getEyePosition());
        for (int i = ToolSparks.count(kind, false); i > 0; i--) {
            Vec3 v = ToolSparks.spray(r, face, look, kind);
            if (tip) {
                // fan across the view rather than straight into the camera
                final Vector3fc f = mc.gameRenderer.getMainCamera().forwardVector();
                final double towards = v.x * f.x() + v.y * f.y() + v.z * f.z();
                if (towards < 0) v = v.subtract(f.x() * towards * 0.7, f.y() * towards * 0.7, f.z() * towards * 0.7);
                v = v.scale(0.6);
            }
            final Particle p = mc.particleEngine.createParticle(particle, at.x, at.y, at.z, v.x, v.y, v.z);
            if (tip && p != null) p.scale(0.5F);
        }
    }

    /** First person: the world point behind the drill tip as drawn in hand, so the sparks leave the bit. */
    private static Vec3 tip(Minecraft mc, BlockHitResult hit) {
        final Camera cam = mc.gameRenderer.getMainCamera();
        final Vec3 eye = cam.position();
        final double d = Math.min(TIP_DEPTH, eye.distanceTo(hit.getLocation()) * 0.85);
        final double tanV = Math.tan(Math.toRadians(cam.getFov() / 2.0));
        final double aspect = (double) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        final double side = (mc.player.getMainArm() == HumanoidArm.RIGHT ? -1 : 1) * TIP_X * tanV * aspect * d;
        final double up = TIP_Y * tanV * d;
        final Vector3fc f = cam.forwardVector(), u = cam.upVector(), l = cam.leftVector();
        return eye.add(f.x() * d + l.x() * side + u.x() * up, f.y() * d + l.y() * side + u.y() * up, f.z() * d + l.z() * side + u.z() * up);
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
