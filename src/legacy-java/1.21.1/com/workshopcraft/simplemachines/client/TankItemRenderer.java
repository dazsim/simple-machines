package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.TankContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Draws a tank item as the tank block with its fluid inside, the same as the placed tank. */
public class TankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static TankItemRenderer INSTANCE;

    public TankItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static TankItemRenderer get() {
        if (INSTANCE == null) INSTANCE = new TankItemRenderer();
        return INSTANCE;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        final Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().renderSingleBlock(SimpleMachines.tank.defaultBlockState(), pose, buffers, light, overlay);
        final TankContents c = stack.get(SimpleMachines.TANK);
        if (c == null || c.amount() <= 0) return;
        final Fluid f = c.fluid();
        final int tint = mc.level == null || mc.player == null ? 0xFFFFFFFF : ConduitRenderer.fluidTint(f, mc.level, mc.player.blockPosition());
        final boolean glow = f.defaultFluidState().createLegacyBlock().getLightEmission() > 0;
        TankRenderer.submitFluid(pose, new Submit(buffers), ConduitRenderer.fluidSprite(f), tint, TankRenderer.top(c.amount()), glow ? 0xF000F0 : light);
    }
}
