package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional parts screwed in after the control board, two slots per machine, three on entangled devices, any mix.
 * Each Overclocker adds half the base speed (FE use and intake rise with it); each Energy Cell adds the base storage again.
 * Modules only fit entangled buffers, tanks and relays: one Chunk Loader, then up to two Range Modules.
 */
public enum Upgrade {
    OVERCLOCKER("overclocker_circuit", false),
    ENERGY_CELL("energy_cell", false),
    CHUNK_LOADER("chunk_loader_module", true),
    RANGE("range_module", true);

    public static final int SLOTS = 2, MAX_SLOTS = 3, MAX_RANGE = 2;
    public final String id;
    public final boolean module;

    Upgrade(String id, boolean module) {
        this.id = id;
        this.module = module;
    }

    public static boolean loads(Core core) {
        return core == Core.BUFFER || core == Core.TANK || core.relay();
    }

    public static int slots(@Nullable Core core) {
        return core != null && loads(core) ? MAX_SLOTS : SLOTS;
    }

    /** Why this part can't go into a chassis of the given core that already holds these, or null when it fits. */
    public @Nullable String refusal(Core core, List<Upgrade> fitted) {
        if (fitted.size() >= slots(core)) return "message.simplemachines.upgrades_full";
        if (this == OVERCLOCKER && core.entangled()) return "message.simplemachines.no_overclock_entangled";
        if (!module) return null;
        if (!loads(core)) return "message.simplemachines.module_no_loader";
        if (this == CHUNK_LOADER) return fitted.contains(CHUNK_LOADER) ? "message.simplemachines.module_one_loader" : null;
        if (!fitted.contains(CHUNK_LOADER)) return "message.simplemachines.module_needs_loader";
        return count(fitted, RANGE) >= MAX_RANGE ? "message.simplemachines.module_range_full" : null;
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
            if (u != null && out.size() < MAX_SLOTS) out.add(u);
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

    /** Two bits per kind in declaration order, for a menu data slot. */
    public static int pack(List<Upgrade> list) {
        int out = 0;
        for (Upgrade u : values()) out |= Math.min(3, count(list, u)) << 2 * u.ordinal();
        return out;
    }

    public static int count(int packed, Upgrade u) {
        return packed >> 2 * u.ordinal() & 3;
    }

    /** "Overclocker Circuit x2, Energy Cell" style summary of upgrades or of modules, empty when there are none. */
    public static Component summary(int packed, boolean brief) {
        return summary(packed, brief, false);
    }

    public static Component summary(int packed, boolean brief, boolean modules) {
        Component out = null;
        for (Upgrade u : values()) {
            final int c = count(packed, u);
            if (c == 0 || u.module != modules) continue;
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
