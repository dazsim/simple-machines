package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Material of the side panels and the worktop. */
public enum Finish implements StringRepresentable {
    NONE("none"),
    WOOD("wood"),
    IRON("iron"),
    COPPER("copper"),
    STEEL("steel");

    private final String name;

    Finish(String name) {
        this.name = name;
    }

    public Item panels() {
        return SimpleMachines.PANELS.get(this);
    }

    public Item worktop() {
        return SimpleMachines.WORKTOPS.get(this);
    }

    public static Finish panelsOf(ItemStack stack) {
        for (Finish f : values()) if (f != NONE && stack.is(f.panels())) return f;
        return NONE;
    }

    public static Finish worktopOf(ItemStack stack) {
        for (Finish f : values()) if (f != NONE && stack.is(f.worktop())) return f;
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
