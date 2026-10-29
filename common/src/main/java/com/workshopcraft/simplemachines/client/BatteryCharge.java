package com.workshopcraft.simplemachines.client;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.BatteryBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model range simplemachines:charge, the battery gauge 0 to 5 bars, the same as the placed block. */
public record BatteryCharge() implements RangeSelectItemModelProperty {
    public static final MapCodec<BatteryCharge> MAP_CODEC = MapCodec.unit(new BatteryCharge());

    public static int bars(ItemStack stack) {
        final long cap = BatteryBlockEntity.itemCapacity(stack);
        final long e = stack.getOrDefault(SimpleMachines.ENERGY, 0);
        return cap <= 0 ? 0 : (int) Math.min(5, 5L * e / cap);
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return bars(stack);
    }

    @SuppressWarnings("unchecked")
    public static void registerDirect(net.minecraft.resources.Identifier id) {
        try {
            final java.lang.reflect.Field f = net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.class.getDeclaredField("ID_MAPPER");
            f.setAccessible(true);
            ((net.minecraft.util.ExtraCodecs.LateBoundIdMapper<net.minecraft.resources.Identifier, MapCodec<? extends RangeSelectItemModelProperty>>) f.get(null)).put(id, MAP_CODEC);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("charge model property", e);
        }
    }

    @Override
    public MapCodec<BatteryCharge> type() {
        return MAP_CODEC;
    }
}
