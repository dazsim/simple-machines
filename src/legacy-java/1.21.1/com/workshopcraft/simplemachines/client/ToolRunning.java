package com.workshopcraft.simplemachines.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item model predicate simplemachines:tool_running, the main-hand tool's spin speed. */
public final class ToolRunning implements ClampedItemPropertyFunction {
    public static final ToolRunning INSTANCE = new ToolRunning();

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (entity == null || !main(entity.getMainHandItem(), stack)) return 0;
        return ToolEffects.speed(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) > 0 ? 1 : 0;
    }

    private static boolean main(ItemStack held, ItemStack stack) {
        return held == stack || ItemStack.isSameItemSameComponents(held, stack);
    }
}
