package com.workshopcraft.simplemachines.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item model predicate simplemachines:spin, the bit frame 0 to 5 advancing with the holder's spin speed. */
public final class ToolSpin implements ClampedItemPropertyFunction {
    public static final ToolSpin INSTANCE = new ToolSpin();
    public static final int FRAMES = 6;

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return ToolEffects.phase(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    @Override
    public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return unclampedCall(stack, level, entity, seed);
    }
}
