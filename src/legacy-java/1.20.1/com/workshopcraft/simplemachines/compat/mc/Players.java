package com.workshopcraft.simplemachines.compat.mc;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Players {
    private Players() {
    }

    public static void overlay(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    public static double reach(Player player) {
        return player.isCreative() ? 5.0 : 4.5;
    }
}
