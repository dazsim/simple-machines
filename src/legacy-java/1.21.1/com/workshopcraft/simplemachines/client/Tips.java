package com.workshopcraft.simplemachines.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Deferred screen tooltips, standing in for the newer setTooltipForNextFrame. */
final class Tips {
    private Tips() {
    }

    static void show(Component c) {
        show(List.of(c));
    }

    static void show(List<Component> lines) {
        if (Minecraft.getInstance().screen != null)
            Minecraft.getInstance().screen.setTooltipForNextRenderPass(lines.stream().map(Component::getVisualOrderText).toList());
    }
}
