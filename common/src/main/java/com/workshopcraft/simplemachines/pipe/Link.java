package com.workshopcraft.simplemachines.pipe;

import net.minecraft.util.StringRepresentable;

/** What a pipe arm joins: nothing, another pipe, an inventory it delivers to, pulls from, or is disconnected from. */
public enum Link implements StringRepresentable {
    NONE("none"),
    PIPE("pipe"),
    LINK("link"),
    EXTRACT("extract"),
    OFF("off");

    private final String name;

    Link(String name) {
        this.name = name;
    }

    /** Delivers to or pulls from the inventory on this side. */
    public boolean inventory() {
        return this == LINK || this == EXTRACT;
    }

    /** Faces an inventory, connected or not. */
    public boolean end() {
        return inventory() || this == OFF;
    }

    /** Has an arm drawn and a hit box. */
    public boolean drawn() {
        return this != NONE && this != OFF;
    }

    /** Screwdriver order: input, output, disconnected. */
    public Link cycle() {
        return switch (this) {
            case LINK -> EXTRACT;
            case EXTRACT -> OFF;
            case OFF -> LINK;
            default -> this;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
