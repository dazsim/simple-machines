package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;

/** Shared wording for times and energy on the recipe pages. */
public final class JeiText {
    private static final DecimalFormat SECONDS = new DecimalFormat("0.##");
    private static final DecimalFormat FE = new DecimalFormat("#,###");

    private JeiText() {
    }

    public static String seconds(int ticks) {
        return SECONDS.format(ticks / 20.0);
    }

    public static String fe(long amount) {
        return FE.format(amount);
    }

    /** "Crusher: 10 s, 4,000 FE" for a machine on an iron chassis. */
    public static Component powered(Component machine, int ticks, int perTick) {
        return line(machine, Component.translatable("jei.simplemachines.powered", seconds(ticks), fe((long) ticks * perTick)));
    }

    public static Component perTick(int perTick) {
        return Component.translatable("jei.simplemachines.per_tick", perTick);
    }

    public static Component line(Component machine, Component text) {
        return Component.translatable("jei.simplemachines.line", machine, text);
    }

    public static Component steel(int ticks, int perTick) {
        final int t = Tier.STEEL.time(ticks);
        return Component.translatable("jei.simplemachines.steel_time", seconds(t), fe((long) t * perTick));
    }

    public static Component fuel(int ticks) {
        return Component.translatable("jei.simplemachines.fuel_time", seconds(ticks));
    }
}
