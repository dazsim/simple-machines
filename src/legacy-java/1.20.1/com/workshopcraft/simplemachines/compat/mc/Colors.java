package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.world.item.DyeColor;

public final class Colors {
    private Colors() {
    }

    public static int diffuse(DyeColor c) {
        final float[] f = c.getTextureDiffuseColors();
        return ((int) (f[0] * 255) << 16) | ((int) (f[1] * 255) << 8) | (int) (f[2] * 255);
    }
}
