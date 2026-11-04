package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.BatteryBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Item model predicate simplemachines:charge, the battery gauge 0 to 5 bars, the same as the placed block. */
public final class BatteryCharge implements ClampedItemPropertyFunction {
    public static final BatteryCharge INSTANCE = new BatteryCharge();

    public static int bars(ItemStack stack) {
        final long cap = BatteryBlockEntity.itemCapacity(stack);
        final long e = stack.getOrDefault(SimpleMachines.ENERGY, 0);
        return cap <= 0 ? 0 : (int) Math.min(5, 5L * e / cap);
    }

    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return bars(stack);
    }

    @Override
    public float call(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        return bars(stack);
    }
}
