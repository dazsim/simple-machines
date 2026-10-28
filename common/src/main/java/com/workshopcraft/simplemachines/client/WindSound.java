package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.gen.WindTurbineBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Blade whoosh and hum from a turning wind turbine, played at the middle of its blade stack.
 * Only the nearest few turbines play so a wind farm stays a hum rather than a roar.
 */
public final class WindSound extends AbstractTickableSoundInstance {
    static final int VOICES = 2;
    private static final Map<WindTurbineBlockEntity, WindSound> LOOPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<WindTurbineBlockEntity, Boolean> SEEN = Collections.synchronizedMap(new WeakHashMap<>());

    private final WindTurbineBlockEntity be;
    private boolean wanted = true;

    private WindSound(WindTurbineBlockEntity be) {
        super(SimpleMachines.WIND_SOUND, SoundSource.BLOCKS, RandomSource.create());
        this.be = be;
        looping = true;
        update();
    }

    static void seen(WindTurbineBlockEntity be) {
        SEEN.put(be, Boolean.TRUE);
    }

    /** Loudness for a stack of blades: grows with the stack and stops at a quiet cap. */
    public static float volume(int blades) {
        return blades <= 0 ? 0 : Math.min(0.32F, 0.12F + 0.04F * blades);
    }

    /** Playback rate for a turn speed in degrees per tick; faster blades give a higher, quicker whoosh. */
    public static float pitch(float speed) {
        return Mth.clamp(0.55F + speed * 0.04F, 0.6F, 1.45F);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            synchronized (LOOPS) {
                LOOPS.values().forEach(mc.getSoundManager()::stop);
                LOOPS.clear();
            }
            SEEN.clear();
            return;
        }
        final Vec3 eye = mc.player.getEyePosition();
        final List<WindTurbineBlockEntity> turning = new ArrayList<>();
        synchronized (SEEN) {
            for (WindTurbineBlockEntity be : SEEN.keySet().toArray(new WindTurbineBlockEntity[0])) {
                if (be.isRemoved() || be.getLevel() != mc.level) {
                    SEEN.remove(be);
                    continue;
                }
                if (be.speed > 0.3F && be.blades() > 0) turning.add(be);
            }
        }
        turning.sort(Comparator.comparingDouble(be -> be.getBlockPos().getCenter().distanceToSqr(eye)));
        final List<WindTurbineBlockEntity> play = turning.subList(0, Math.min(VOICES, turning.size()));
        synchronized (LOOPS) {
            for (Map.Entry<WindTurbineBlockEntity, WindSound> e : LOOPS.entrySet()) {
                if (!play.contains(e.getKey())) e.getValue().wanted = false;
            }
        }
        for (WindTurbineBlockEntity be : play) {
            final WindSound cur = LOOPS.get(be);
            if (cur == null || cur.isStopped()) {
                final WindSound s = new WindSound(be);
                LOOPS.put(be, s);
                mc.getSoundManager().play(s);
            } else {
                cur.wanted = true;
            }
        }
    }

    private void update() {
        final int n = be.blades();
        x = be.getBlockPos().getX() + 0.5;
        y = be.getBlockPos().getY() + 1 + n / 2.0;
        z = be.getBlockPos().getZ() + 0.5;
        volume = volume(n);
        pitch = pitch(be.speed);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (!wanted || be.isRemoved() || be.getLevel() == null || be.speed <= 0.3F || be.blades() == 0) {
            stop();
            synchronized (LOOPS) {
                if (LOOPS.get(be) == this) LOOPS.remove(be);
            }
            return;
        }
        update();
    }
}
