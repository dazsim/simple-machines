package com.workshopcraft.simplemachines.pipe;

import net.minecraft.util.StringRepresentable;

/** What a pipe arm joins: nothing, another pipe, an inventory it delivers to, or an inventory it pulls from. */
public enum Link implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    LINK("link"),
    EXTRACT("extract");

    private final String name;

    Link(String name) {
        this.name = name;
    }

    public boolean inventory() {
        return this == LINK || this == EXTRACT;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
