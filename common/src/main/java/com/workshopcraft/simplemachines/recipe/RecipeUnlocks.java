package com.workshopcraft.simplemachines.recipe;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class RecipeUnlocks {
    private RecipeUnlocks() {}

    // recipe unlocks only fire when an item enters the inventory, so items carried over from
    // an older version never unlock newer recipes; recheck what the player holds on login
    public static void onLogin(ServerPlayer player) {
        final Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            final ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) CriteriaTriggers.INVENTORY_CHANGED.trigger(player, inv, stack);
        }
    }
}
