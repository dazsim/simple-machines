package com.workshopcraft.simplemachines.compat.mc.component;

import net.minecraft.world.item.DyeColor;
import com.workshopcraft.simplemachines.compat.mc.item.BlockItemStateProperties;

/** Vanilla components the mod reads, mapped onto 1.20.1 item data by Comps. */
public final class DataComponents {
    public static final DataComponentType<BlockItemStateProperties> BLOCK_STATE = new DataComponentType<>(null, "BlockStateTag");
    public static final DataComponentType<DyeColor> DYE = new DataComponentType<>(null, "$dye");

    private DataComponents() {
    }
}
