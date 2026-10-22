package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The mechanical assembly fitted first; it decides which machine the chassis becomes. */
public enum Core implements StringRepresentable {
    NONE("none"),
    FURNACE("furnace"),
    ROLLING("rolling"),
    GENERATOR("generator");

    private final String name;

    Core(String name) {
        this.name = name;
    }

    public Item item() {
        return SimpleMachines.ASSEMBLIES.get(this);
    }

    public Block machine() {
        return switch (this) {
            case FURNACE -> SimpleMachines.electricFurnace;
            case ROLLING -> SimpleMachines.rollingMill;
            case GENERATOR -> SimpleMachines.generator;
            case NONE -> throw new IllegalStateException();
        };
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
