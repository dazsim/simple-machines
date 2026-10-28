package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.item.PoweredToolItem;
import com.workshopcraft.simplemachines.item.ToolSparks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;

/** Client tick: spin-up and wind-down of held tools with a looping motor sound, and sparks or dust at the crosshair. */
public final class ToolEffects {
    /** Ticks a tool keeps full speed after it stops digging, so the next block starts without a restart. */
    public static final int GRACE = 10;
    public static final float UP = 1F / 9, DOWN = 1F / 16;
    public static final float LOW_PITCH = 0.5F, VOLUME = 0.6F;
    private static final boolean LOG = Boolean.getBoolean("simplemachines.spinlog");
    private static final Int2ObjectMap<Spin> SPINS = new Int2ObjectOpenHashMap<>();
    private static Field destroying;
    private static boolean noRemote;
    private static int age;

    private ToolEffects() {
    }

    public static void tick(Minecraft mc) {
        QuarrySound.tick(mc);
        if (mc.player == null || mc.level == null) {
            SPINS.values().forEach(Spin::halt);
            SPINS.clear();
            return;
        }
        if (mc.isPaused()) return;
        final PoweredToolItem tool = digging(mc);
        spin(mc.player).tick(mc.player, tool);
        final Int2ObjectMap<BlockDestructionProgress> remote = remote(mc);
        for (Player p : mc.level.players()) {
            if (p != mc.player && (remote != null && remote.containsKey(p.getId()) || SPINS.containsKey(p.getId())))
                spin(p).tick(p, remote == null ? null : remoteDigging(mc, p, remote.get(p.getId())));
        }
        if (SPINS.size() > 1) SPINS.int2ObjectEntrySet().removeIf(e -> {
            final Spin s = e.getValue();
            final boolean gone = s.owner != mc.player && (s.idle() || mc.level.getEntity(e.getIntKey()) == null);
            if (gone) s.halt();
            return gone;
        });
        if (tool != null && ++age % 2 == 0 && mc.hitResult instanceof BlockHitResult hit) particles(mc, tool, hit);
    }

    private static Spin spin(Player p) {
        return SPINS.computeIfAbsent(p.getId(), id -> new Spin(p));
    }

    /** Spin speed 0 to 1 of this entity's main-hand tool, eased between ticks. */
    public static float speed(@Nullable Entity e, float partial) {
        final Spin s = e == null ? null : SPINS.get(e.getId());
        return s == null ? 0 : Mth.lerp(partial, s.prev, s.speed);
    }

    /** Bit frame for the spin model, 0 until FRAMES. */
    public static float phase(@Nullable Entity e, float partial) {
        final Spin s = e == null ? null : SPINS.get(e.getId());
        return s == null ? 0 : (float) ((s.phase + s.speed * partial) % ToolSpin.FRAMES);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Int2ObjectMap<BlockDestructionProgress> remote(Minecraft mc) {
        if (noRemote) return null;
        try {
            if (destroying == null) {
                destroying = LevelRenderer.class.getDeclaredField("destroyingBlocks");
                destroying.setAccessible(true);
            }
            return (Int2ObjectMap<BlockDestructionProgress>) destroying.get(mc.levelRenderer);
        } catch (ReflectiveOperationException | RuntimeException e) {
            noRemote = true;
            com.mojang.logging.LogUtils.getLogger().warn("No dig progress for remote tool sound", e);
            return null;
        }
    }

    private static @Nullable PoweredToolItem remoteDigging(Minecraft mc, Player p, @Nullable BlockDestructionProgress at) {
        if (at == null) return null;
        final ItemStack stack = p.getMainHandItem();
        if (!(stack.getItem() instanceof PoweredToolItem tool) || !PoweredToolItem.powered(stack)) return null;
        final BlockState state = mc.level.getBlockState(at.getPos());
        return tool.getDestroySpeed(stack, state) > 1.0F || tool.refuses(stack, state) ? tool : null;
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
        final boolean near = kind != ToolSparks.Kind.DUST && mc.options.getCameraType().isFirstPerson();
        final Vec3 at = ToolSparks.contact(hit, mc.player.getEyePosition());
        for (int i = ToolSparks.count(kind, false); i > 0; i--) {
            Vec3 v = ToolSparks.spray(r, face, look, kind);
            if (near) {
                // fan across the view rather than straight into the camera
                final Vector3fc f = mc.gameRenderer.getMainCamera().forwardVector();
                final double towards = v.x * f.x() + v.y * f.y() + v.z * f.z();
                if (towards < 0) v = v.subtract(f.x() * towards * 0.7, f.y() * towards * 0.7, f.z() * towards * 0.7);
                v = v.scale(0.6);
            }
            final Particle p = mc.particleEngine.createParticle(particle, at.x, at.y, at.z, v.x, v.y, v.z);
            if (near && p != null) p.scale(0.5F);
        }
    }

    /** One player's tool: speed ramps up while digging, holds for GRACE ticks after, then winds down. */
    private static final class Spin {
        Player owner;
        PoweredToolItem tool;
        float speed, prev;
        double phase;
        int since = GRACE + 1;
        Loop loop;

        Spin(Player owner) {
            this.owner = owner;
        }

        void tick(Player p, @Nullable PoweredToolItem digging) {
            owner = p;
            prev = speed;
            final ItemStack held = p.getMainHandItem();
            final PoweredToolItem in = held.getItem() instanceof PoweredToolItem t && PoweredToolItem.powered(held) ? t : null;
            if (digging != null) {
                if (digging != tool) speed = prev = 0;
                tool = digging;
                since = 0;
            } else since++;
            if (in == null || in != tool) speed = prev = 0;
            else speed = since <= GRACE ? Math.min(1F, speed + UP) : Math.max(0F, speed - DOWN);
            phase = (phase + speed) % ToolSpin.FRAMES;
            final Minecraft mc = Minecraft.getInstance();
            if (speed > 0 && (loop == null || loop.isStopped() || loop.tool != tool || !mc.getSoundManager().isActive(loop))) {
                if (loop != null) loop.end();
                loop = new Loop(this, tool);
                mc.getSoundManager().play(loop);
            }
            if (LOG && p == mc.player && (speed > 0 || prev > 0))
                com.mojang.logging.LogUtils.getLogger().info("SMSPIN t={} speed={} dig={} since={} pitch={} snd={}", mc.level.getGameTime(),
                        String.format(java.util.Locale.ROOT, "%.3f", speed), digging != null, since,
                        String.format(java.util.Locale.ROOT, "%.2f", pitch()), loop != null && mc.getSoundManager().isActive(loop));
        }

        float pitch() {
            return LOW_PITCH + (1F - LOW_PITCH) * speed;
        }

        boolean idle() {
            return speed <= 0 && prev <= 0 && since > GRACE;
        }

        void halt() {
            if (loop != null) loop.end();
        }
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        final Spin spin;
        final PoweredToolItem tool;

        Loop(Spin spin, PoweredToolItem tool) {
            super(tool.sound, SoundSource.PLAYERS, RandomSource.create());
            this.spin = spin;
            this.tool = tool;
            looping = true;
            update();
        }

        void end() {
            stop();
        }

        private void update() {
            final Player p = spin.owner;
            x = p.getX();
            y = p.getEyeY();
            z = p.getZ();
            pitch = spin.pitch();
            volume = VOLUME * Math.min(1F, spin.speed / 0.45F);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (spin.owner.isRemoved() || spin.loop != this || spin.speed <= 0) {
                stop();
                return;
            }
            update();
        }
    }
}
