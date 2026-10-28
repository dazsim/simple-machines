package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.quarry.QuarryBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** The electric drill's run loop, played at a quarry head while it cuts into a block. */
public final class QuarrySound extends AbstractTickableSoundInstance {
    private static final Map<QuarryBlockEntity, QuarrySound> LOOPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<QuarryBlockEntity, Boolean> SEEN = Collections.synchronizedMap(new WeakHashMap<>());

    private final QuarryBlockEntity be;

    private QuarrySound(QuarryBlockEntity be) {
        super(SimpleMachines.DRILL_SOUND, SoundSource.BLOCKS, RandomSource.create());
        this.be = be;
        looping = true;
        volume = 0.8F;
        pitch = 0.85F;
        place();
    }

    /** Called by the renderer so the tick knows which quarries are around. */
    static void seen(QuarryBlockEntity be) {
        SEEN.put(be, Boolean.TRUE);
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            synchronized (LOOPS) {
                LOOPS.values().forEach(mc.getSoundManager()::stop);
                LOOPS.clear();
            }
            SEEN.clear();
            return;
        }
        final QuarryBlockEntity[] all;
        synchronized (SEEN) {
            all = SEEN.keySet().toArray(new QuarryBlockEntity[0]);
        }
        for (QuarryBlockEntity be : all) {
            if (be.isRemoved() || be.getLevel() != mc.level) {
                SEEN.remove(be);
                continue;
            }
            final QuarrySound cur = LOOPS.get(be);
            if ((cur == null || cur.isStopped()) && cutting(be)) {
                final QuarrySound s = new QuarrySound(be);
                LOOPS.put(be, s);
                mc.getSoundManager().play(s);
            }
        }
    }

    private static boolean cutting(QuarryBlockEntity be) {
        final float t = be.getLevel().getGameTime();
        return be.tip(t) != null && be.drillingAt(t);
    }

    private void place() {
        final double[] tip = be.getLevel() == null ? null : be.tip(be.getLevel().getGameTime());
        if (tip == null) return;
        x = tip[0];
        y = tip[1] + 0.4;
        z = tip[2];
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (be.isRemoved() || be.getLevel() == null || !cutting(be)) {
            stop();
            return;
        }
        place();
    }
}
