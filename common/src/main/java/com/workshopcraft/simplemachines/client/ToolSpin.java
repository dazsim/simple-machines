package com.workshopcraft.simplemachines.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Item model range simplemachines:spin, the bit frame 0 to 5 advancing with the holder's spin speed. */
public record ToolSpin() implements RangeSelectItemModelProperty {
    public static final MapCodec<ToolSpin> MAP_CODEC = MapCodec.unit(new ToolSpin());
    public static final int FRAMES = 6;

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return ToolEffects.phase(owner == null ? null : owner.asLivingEntity(), Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    /** Fabric has no hook for numeric item model properties, so the vanilla id map is filled directly. */
    @SuppressWarnings("unchecked")
    public static void registerDirect(net.minecraft.resources.Identifier id) {
        try {
            final java.lang.reflect.Field f = net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties.class.getDeclaredField("ID_MAPPER");
            f.setAccessible(true);
            ((net.minecraft.util.ExtraCodecs.LateBoundIdMapper<net.minecraft.resources.Identifier, MapCodec<? extends RangeSelectItemModelProperty>>) f.get(null)).put(id, MAP_CODEC);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("spin model property", e);
        }
    }

    @Override
    public MapCodec<ToolSpin> type() {
        return MAP_CODEC;
    }
}
