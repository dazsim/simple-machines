package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The fluid seen through the tank glass, filled to its level. */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity, TankRenderer.State> {
    private static final float LO = 1.02F, HI = 14.98F;

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int tint;
        float top;
        boolean glow;
    }

    public TankRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TankBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, breaking);
        s.sprite = null;
        if (be.amount() <= 0) return;
        final Fluid f = be.fluid();
        s.sprite = ConduitRenderer.fluidSprite(f);
        s.tint = Minecraft.getInstance().level == null ? 0xFFFFFFFF : ConduitRenderer.fluidTint(f, Minecraft.getInstance().level, be.getBlockPos());
        s.top = LO + Math.max(0.25F, (HI - LO) * be.amount() / (float) TankBlockEntity.CAPACITY);
        s.glow = f.defaultFluidState().createLegacyBlock().getLightEmission() > 0;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.sprite == null) return;
        final TextureAtlasSprite sp = s.sprite;
        final int light = s.glow ? LightCoordsUtil.FULL_BRIGHT : s.lightCoords;
        out.submitCustomGeometry(pose, Sheets.translucentBlockSheet(), (p, vc) -> {
            final Quads q = new Quads(p, vc, light, OverlayTexture.NO_OVERLAY);
            final float t = s.top;
            q.face(Direction.NORTH, LO, LO, LO, HI, t, HI, sp, LO, 16 - t, HI, 16 - LO, s.tint);
            q.face(Direction.SOUTH, LO, LO, LO, HI, t, HI, sp, LO, 16 - t, HI, 16 - LO, s.tint);
            q.face(Direction.EAST, LO, LO, LO, HI, t, HI, sp, LO, 16 - t, HI, 16 - LO, s.tint);
            q.face(Direction.WEST, LO, LO, LO, HI, t, HI, sp, LO, 16 - t, HI, 16 - LO, s.tint);
            q.face(Direction.UP, LO, LO, LO, HI, t, HI, sp, LO, LO, HI, HI, s.tint);
            q.face(Direction.DOWN, LO, LO, LO, HI, t, HI, sp, LO, LO, HI, HI, s.tint);
        });
    }
}
