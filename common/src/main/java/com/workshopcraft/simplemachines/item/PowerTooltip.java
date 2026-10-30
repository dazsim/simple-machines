package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.cable.PowerInfo;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.PoweredMachineBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** The tier, use and energy lines on our powered items. */
public final class PowerTooltip {
    private PowerTooltip() {
    }

    public static String num(int n) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(n);
    }

    /** "LV" in the colour of its cable. */
    public static Component tier(int tier) {
        return Component.literal(Voltage.NAME[tier]).withColor(Voltage.COLOR[tier]);
    }

    private static MutableComponent line(String key, Object... args) {
        return Component.translatable("tooltip.simplemachines." + key, args).withStyle(ChatFormatting.GRAY);
    }

    public static void append(ItemStack stack, Block block, Consumer<Component> out) {
        final PowerInfo p = PowerInfo.of(block);
        if (p == null) return;
        if (p.unit() == PowerInfo.Unit.CABLE) {
            out.accept(line("carries", tier(p.accepts()), num(p.out())));
            return;
        }
        if (p.unit() == PowerInfo.Unit.TRANSFORMER) {
            out.accept(line("transformer_low", tier(p.accepts()), num(p.out())));
            out.accept(line("transformer_high", tier(p.sends())));
            energy(stack, p.capacity() * 2, out, line("transformer_holds", num(p.capacity())));
            out.accept(line("transformer_screwdriver", tier(p.sends())));
            return;
        }
        Tier tier = Tier.IRON;
        int oc = 0, cells = 0;
        if (p.chassis()) {
            final Tier t = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(PoweredMachineBlock.TIER);
            if (t != null) tier = t;
            final List<Upgrade> ups = Upgrade.parse(stack.getOrDefault(SimpleMachines.UPGRADES_FITTED, ""));
            oc = Upgrade.count(ups, Upgrade.OVERCLOCKER);
            cells = Upgrade.count(ups, Upgrade.ENERGY_CELL);
        }
        final int fast = 2 + oc;
        if (p.accepts() >= 0) out.accept(line("accepts", tier(p.accepts()), num(Voltage.RATE[p.accepts()])));
        switch (p.unit()) {
            case RUNNING -> out.accept(line("uses_tick", num(p.use() * fast / 2)));
            case BLOCK -> out.accept(line("uses_block", num(p.use())));
            case WRITE -> out.accept(line("uses_write", num(p.use())));
            case SHEAR -> out.accept(line("uses_shear", num(p.use())));
            case CROP -> out.accept(line("uses_crop", num(p.use())));
            case CHARGE -> out.accept(line("charges_items", num(p.use() * tier.storage * fast / 2)));
            default -> {}
        }
        if (p.sends() >= 0) out.accept(line("sends", tier(p.sends()), num(p.chassis() ? p.out() * tier.storage : p.out())));
        switch (p.unit()) {
            case BURNING -> out.accept(line("makes_burning", num((tier == Tier.IRON ? p.use() : p.steelUse()) * fast / 2)));
            case SUN -> out.accept(p.night() > 0 ? line("makes_sun_night", num(p.use()), num(p.night())) : line("makes_sun", num(p.use())));
            case BLADE -> out.accept(line("makes_blade", num(p.use())));
            default -> {}
        }
        final int cap = p.chassis() ? p.capacity() * tier.storage * (1 + cells) : p.capacity();
        energy(stack, cap, out, line("holds", num(cap)));
    }

    private static void energy(ItemStack stack, int cap, Consumer<Component> out, Component empty) {
        final int e = stack.getOrDefault(SimpleMachines.ENERGY, 0);
        out.accept(e > 0 ? line("energy", num(e), num(cap)) : empty);
    }
}
