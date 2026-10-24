package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Material of the side panels, matching Much More Storage's woods and concretes. */
public enum Finish implements StringRepresentable {
    NONE("none"),
    OAK("oak"),
    SPRUCE("spruce"),
    BIRCH("birch"),
    JUNGLE("jungle"),
    ACACIA("acacia"),
    DARK_OAK("dark_oak"),
    MANGROVE("mangrove"),
    CHERRY("cherry"),
    PALE_OAK("pale_oak"),
    BAMBOO("bamboo"),
    CRIMSON("crimson"),
    WARPED("warped"),
    WHITE_CONCRETE("white_concrete"),
    ORANGE_CONCRETE("orange_concrete"),
    MAGENTA_CONCRETE("magenta_concrete"),
    LIGHT_BLUE_CONCRETE("light_blue_concrete"),
    YELLOW_CONCRETE("yellow_concrete"),
    LIME_CONCRETE("lime_concrete"),
    PINK_CONCRETE("pink_concrete"),
    GRAY_CONCRETE("gray_concrete"),
    LIGHT_GRAY_CONCRETE("light_gray_concrete"),
    CYAN_CONCRETE("cyan_concrete"),
    PURPLE_CONCRETE("purple_concrete"),
    BLUE_CONCRETE("blue_concrete"),
    BROWN_CONCRETE("brown_concrete"),
    GREEN_CONCRETE("green_concrete"),
    RED_CONCRETE("red_concrete"),
    BLACK_CONCRETE("black_concrete"),
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

    @Override
    public String getSerializedName() {
        return name;
    }
}
