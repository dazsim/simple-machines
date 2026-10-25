package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional parts screwed in after the control board, up to SLOTS per machine, any mix.
 * Each Overclocker adds half the base speed (FE use and intake rise with it); each Energy Cell adds the base storage again.
 */
public enum Upgrade {
    OVERCLOCKER("overclocker_circuit"),
    ENERGY_CELL("energy_cell");

    public static final int SLOTS = 2;
    public final String id;

    Upgrade(String id) {
        this.id = id;
    }

    public Item item() {
        return SimpleMachines.UPGRADES.get(this);
    }

    public static @Nullable Upgrade of(ItemStack stack) {
        for (Upgrade u : values()) if (stack.is(u.item())) return u;
        return null;
    }

    public static @Nullable Upgrade byId(String id) {
        for (Upgrade u : values()) if (u.id.equals(id)) return u;
        return null;
    }

    public static List<Upgrade> parse(String s) {
        final List<Upgrade> out = new ArrayList<>();
        for (String part : s.split(",")) {
            final Upgrade u = byId(part.trim());
            if (u != null && out.size() < SLOTS) out.add(u);
        }
        return out;
    }

    public static String join(List<Upgrade> list) {
        final StringBuilder b = new StringBuilder();
        for (Upgrade u : list) b.append(b.isEmpty() ? "" : ",").append(u.id);
        return b.toString();
    }

    public static int count(List<Upgrade> list, Upgrade u) {
        int n = 0;
        for (Upgrade x : list) if (x == u) n++;
        return n;
    }

    /** Overclockers in the low bits, cells above, for a menu data slot. */
    public static int pack(List<Upgrade> list) {
        return count(list, OVERCLOCKER) | count(list, ENERGY_CELL) << 4;
    }

    /** "Overclocker Circuit x2, Energy Cell" style summary, empty when there are none. */
    public static Component summary(int packed, boolean brief) {
        final int[] n = {packed & 15, packed >> 4 & 15};
        Component out = null;
        for (Upgrade u : values()) {
            final int c = n[u.ordinal()];
            if (c == 0) continue;
            Component name = Component.translatable(brief ? "upgrade.simplemachines." + u.id : "item.simplemachines." + u.id);
            if (c > 1) name = Component.translatable("upgrade.simplemachines.times", name, c);
            out = out == null ? name : Component.translatable("upgrade.simplemachines.and", out, name);
        }
        return out == null ? Component.empty() : out;
    }

    public interface Holder {
        List<Upgrade> upgrades();

        void setUpgrades(List<Upgrade> upgrades);
    }
}
