package com.workshopcraft.simplemachines.quarry;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/** The electric drill built into a quarry, kept as "frame;tip;upgrades" on the assembly, the chassis and the quarry. */
public record QuarryDrill(boolean steel, boolean diamond, List<Upgrade> upgrades) {
    public static final QuarryDrill PLAIN = new QuarryDrill(false, false, List.of());

    public static QuarryDrill of(ItemStack drill) {
        return new QuarryDrill(PoweredToolItem.steel(drill), PoweredToolItem.diamond(drill), PoweredToolItem.upgrades(drill));
    }

    public static QuarryDrill parse(String s) {
        if (s == null || s.isEmpty()) return PLAIN;
        final String[] p = s.split(";", -1);
        return new QuarryDrill("steel".equals(p[0]), p.length > 1 && "diamond".equals(p[1]), Upgrade.parse(p.length > 2 ? p[2] : ""));
    }

    public String join() {
        return (steel ? "steel" : "iron") + ";" + (diamond ? "diamond" : "standard") + ";" + Upgrade.join(upgrades);
    }

    public float bonus() {
        return Math.min(PoweredToolItem.MAX_UPGRADE_BONUS,
                0.5F * Upgrade.count(upgrades, Upgrade.OVERCLOCKER) + 0.15F * Upgrade.count(upgrades, Upgrade.ENERGY_CELL));
    }

    /** Blocks per second. */
    public float speed(Tier tier) {
        return tier.speed * (steel ? 1.25F : 1F) * (1F + bonus());
    }

    public static String get(ItemStack stack) {
        return stack.getOrDefault(SimpleMachines.QUARRY_DRILL, "");
    }

    public static void tooltip(ItemStack stack, Consumer<Component> out) {
        final String s = get(stack);
        if (s.isEmpty()) return;
        final QuarryDrill d = parse(s);
        out.accept(Component.translatable("tooltip.simplemachines.quarry_drill",
                Component.translatable("tooltip.simplemachines.frame_" + (d.steel ? "steel" : "iron")),
                Component.translatable("tooltip.simplemachines.tip_" + (d.diamond ? "diamond" : "standard")),
                Math.round(d.bonus() * 100)).withStyle(ChatFormatting.GRAY));
    }
}
