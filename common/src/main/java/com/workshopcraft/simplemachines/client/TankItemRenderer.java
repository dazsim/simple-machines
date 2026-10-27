package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.TankContents;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/** Draws the fluid inside a tank item, the same as the placed tank. */
public class TankItemRenderer implements SpecialModelRenderer<TankContents> {
    @Override
    public @Nullable TankContents extractArgument(ItemStack stack) {
        final TankContents c = stack.get(SimpleMachines.TANK);
        return c == null || c.amount() <= 0 ? null : c;
    }

    @Override
    public void submit(@Nullable TankContents c, PoseStack pose, SubmitNodeCollector out, int light, int overlay, boolean foil, int outline) {
        if (c == null) return;
        final Fluid f = c.fluid();
        final Minecraft mc = Minecraft.getInstance();
        final int tint = mc.level == null || mc.player == null ? 0xFFFFFFFF : ConduitRenderer.fluidTint(f, mc.level, mc.player.blockPosition());
        final boolean glow = f.defaultFluidState().createLegacyBlock().getLightEmission() > 0;
        TankRenderer.submitFluid(pose, out, ConduitRenderer.fluidSprite(f), tint, TankRenderer.top(c.amount()), glow ? LightCoordsUtil.FULL_BRIGHT : light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int i = 0; i < 8; i++) output.accept(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<TankContents> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<TankContents> bake(SpecialModelRenderer.BakingContext context) {
            return new TankItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        /** Fabric has no hook for special item renderers, so the vanilla id map is filled directly. */
        @SuppressWarnings("unchecked")
        public static void registerDirect(net.minecraft.resources.Identifier id) {
            try {
                final java.lang.reflect.Field f = net.minecraft.client.renderer.special.SpecialModelRenderers.class.getDeclaredField("ID_MAPPER");
                f.setAccessible(true);
                ((net.minecraft.util.ExtraCodecs.LateBoundIdMapper<net.minecraft.resources.Identifier, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>>) f.get(null)).put(id, MAP_CODEC);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("tank special renderer", e);
            }
        }
    }
}
