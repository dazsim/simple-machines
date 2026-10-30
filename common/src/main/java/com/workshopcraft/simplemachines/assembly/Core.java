package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The mechanical assembly fitted first; it decides which machine the chassis becomes. */
public enum Core implements StringRepresentable {
    NONE("none", ""),
    FURNACE("furnace", "electric_furnace"),
    ROLLING("rolling", "rolling_mill"),
    GENERATOR("generator", "solid_fuel_generator"),
    BATTERY("battery", "battery"),
    CRUSHER("crusher", "crusher"),
    HARVESTER("harvester", "crop_harvester"),
    ALLOY("alloy", "alloy_smelter"),
    STONE("stone", "stone_generator"),
    SAW("saw", "sawmill"),
    WATER("water", "water_generator"),
    HIVE("hive", "mechanical_hive"),
    SHEARER("shearer", "shearer"),
    CHARGER("charger", "charger"),
    CRAFTER("crafter", "auto_crafter"),
    COMPRESSOR("compressor", "compressor"),
    LAVA("lava", "lava_generator"),
    QUARRY("quarry", "quarry"),
    PROGRAMMER("programmer", "ender_programmer");

    private final String name;
    public final String block;

    Core(String name, String block) {
        this.name = name;
        this.block = block;
    }

    public Item item() {
        return SimpleMachines.ASSEMBLIES.get(this);
    }

    public Block machine() {
        final Block b = SimpleMachines.MACHINES.get(this);
        if (b == null) throw new IllegalStateException();
        return b;
    }

    public static Core of(ItemStack stack) {
        for (Core c : values()) if (c != NONE && stack.is(c.item())) return c;
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
