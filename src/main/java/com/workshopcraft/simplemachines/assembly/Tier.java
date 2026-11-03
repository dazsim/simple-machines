package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

/** Chassis tier. Higher tiers work faster and store more. */
public enum Tier implements StringRepresentable {
    IRON("iron", 1.0F, 1),
    STEEL("steel", 1.5F, 2);

    public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

    private final String name;
    public final float speed;
    public final int storage;

    Tier(String name, float speed, int storage) {
        this.name = name;
        this.speed = speed;
        this.storage = storage;
    }

    public int time(int base) {
        return Math.max(1, Math.round(base / speed));
    }

    public Block chassis() {
        return this == STEEL ? SimpleMachines.steelChassis : SimpleMachines.ironChassis;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
