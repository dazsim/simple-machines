package com.workshopcraft.simplemachines.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model condition simplemachines:tool_running, true while a player's main-hand tool spins faster than "above". */
public record ToolRunning(float above) implements ConditionalItemModelProperty {
    public static final MapCodec<ToolRunning> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("above", 0F).forGetter(ToolRunning::above)).apply(i, ToolRunning::new));

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        if (entity == null || context == ItemDisplayContext.GUI || !main(entity.getMainHandItem(), stack)) return false;
        return ToolEffects.speed(entity, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)) > above;
    }

    private static boolean main(ItemStack held, ItemStack stack) {
        return held == stack || ItemStack.isSameItemSameComponents(held, stack);
    }

    /** Fabric has no hook for item model conditions, so the vanilla id map is filled directly. */
    @SuppressWarnings("unchecked")
    public static void registerDirect(net.minecraft.resources.Identifier id) {
        try {
            final java.lang.reflect.Field f = net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties.class.getDeclaredField("ID_MAPPER");
            f.setAccessible(true);
            ((net.minecraft.util.ExtraCodecs.LateBoundIdMapper<net.minecraft.resources.Identifier, MapCodec<? extends ConditionalItemModelProperty>>) f.get(null)).put(id, MAP_CODEC);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("tool_running model condition", e);
        }
    }

    @Override
    public MapCodec<ToolRunning> type() {
        return MAP_CODEC;
    }
}
