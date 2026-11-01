package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.workshopcraft.simplemachines.entangled.EntangledBackpackItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Opens the worn Entangled Backpack, B by default. */
public final class BackpackKey {
    public static final KeyMapping KEY = new KeyMapping(EntangledBackpackItem.OPEN_KEY, InputConstants.KEY_B, KeyMapping.Category.INVENTORY);
    /** Set by the loader: sends the open packet. */
    public static Runnable send = () -> {};

    private BackpackKey() {
    }

    public static void tick(Minecraft mc) {
        while (KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) send.run();
        }
    }
}
