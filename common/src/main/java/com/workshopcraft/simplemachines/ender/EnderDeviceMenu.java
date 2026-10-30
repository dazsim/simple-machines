package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.machine.EnergyMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** Shared data for ender device screens: the energy values, then code, flags, owner in 8 shorts and two extras. */
public abstract class EnderDeviceMenu extends AbstractContainerMenu {
    protected final ContainerData data;

    protected EnderDeviceMenu(@Nullable MenuType<?> type, int id, ContainerData data) {
        super(type, id);
        checkContainerDataCount(data, EnderDeviceBlockEntity.DATA_COUNT);
        this.data = data;
    }

    public static int encode(int k, @Nullable EnderLink link, boolean online, int extra0, int extra1) {
        if (k == 11) return extra0;
        if (k == 12) return extra1;
        if (link == null) return k == 2 && online ? 2 : 0;
        return switch (k) {
            case 0 -> (link.code() + 1) & 0xFFFF;
            case 1 -> (link.code() + 1) >>> 16;
            case 2 -> (link.priv() ? 1 : 0) | (online ? 2 : 0);
            default -> {
                final int i = k - 3;
                final long half = i < 4 ? link.owner().getMostSignificantBits() : link.owner().getLeastSignificantBits();
                yield (int) (half >>> (48 - 16 * (i % 4))) & 0xFFFF;
            }
        };
    }

    private int get(int k) {
        return data.get(EnergyMenu.DATA_COUNT + k) & 0xFFFF;
    }

    public int energy() {
        return (data.get(2) & 0xFFFF) | (data.get(3) & 0xFFFF) << 16;
    }

    public int capacity() {
        return Math.max(1, data.get(4)) * 1000;
    }

    public int voltage() {
        return EnergyMenu.voltage(data.get(6));
    }

    public boolean sendsOnly() {
        return (data.get(6) & 1 << 10) != 0;
    }

    public float energyFraction() {
        return Mth.clamp((float) energy() / capacity(), 0, 1);
    }

    /** The code, or -1 when the device has none. */
    public int code() {
        return (get(0) | get(1) << 16) - 1;
    }

    public boolean isPrivate() {
        return (get(2) & 1) != 0;
    }

    public boolean online() {
        return (get(2) & 2) != 0;
    }

    public UUID owner() {
        long hi = 0, lo = 0;
        for (int i = 0; i < 4; i++) {
            hi = hi << 16 | get(3 + i);
            lo = lo << 16 | get(7 + i);
        }
        return new UUID(hi, lo);
    }

    public int extra(int i) {
        return get(11 + i);
    }
}
