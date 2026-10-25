package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import com.workshopcraft.simplemachines.pipe.ItemPipeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Dyed cables and pipes get a coloured sleeve on the core and a band on each arm. */
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, ConduitRenderer.State> {
    private static final float GROW = 0.3F;

    public static class State extends BlockEntityRenderState {
        int colour;
        int arms;
        float core;
        float arm;
    }

    public ConduitRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ConduitBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, breaking);
        final DyeColor c = be.colour();
        s.colour = c == null ? 0 : c.getTextureDiffuseColor() | 0xFF000000;
        s.arms = 0;
        final BlockState state = be.getBlockState();
        final boolean cable = state.getBlock() instanceof CableBlock;
        s.core = cable ? 2 : 2.5F;
        s.arm = cable ? 2 : 1.5F;
        for (Direction d : Direction.values()) {
            final boolean on = cable ? state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d)) : ItemPipeBlock.link(state, d).drawn();
            if (on) s.arms |= 1 << d.get3DDataValue();
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.colour == 0) return;
        final TextureAtlasSprite sp = Minecraft.getInstance().getAtlasManager()
                .get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, Identifier.withDefaultNamespace("block/white_concrete")));
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> {
            final Quads q = new Quads(p, vc, s.lightCoords, OverlayTexture.NO_OVERLAY);
            final float c0 = 8 - s.core - GROW, c1 = 8 + s.core + GROW;
            cube(q, sp, s.colour, c0, c0, c0, c1, c1, c1);
            final float a0 = 8 - s.arm - GROW, a1 = 8 + s.arm + GROW;
            for (Direction d : Direction.values()) {
                if ((s.arms & 1 << d.get3DDataValue()) == 0) continue;
                // band 1 to 3 px in from the block edge on that side
                final float lo = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 13 : 1;
                final float hi = lo + 2;
                switch (d.getAxis()) {
                    case X -> cube(q, sp, s.colour, lo, a0, a0, hi, a1, a1);
                    case Y -> cube(q, sp, s.colour, a0, lo, a0, a1, hi, a1);
                    case Z -> cube(q, sp, s.colour, a0, a0, lo, a1, a1, hi);
                }
            }
        });
    }

    private static void cube(Quads q, TextureAtlasSprite sp, int colour, float x0, float y0, float z0, float x1, float y1, float z1) {
        for (Direction d : Direction.values()) q.face(d, x0, y0, z0, x1, y1, z1, sp, 0, 0, 4, 4, shade(colour, d));
    }

    private static int shade(int colour, Direction d) {
        final float f = switch (d) {
            case UP -> 1F;
            case DOWN -> 0.5F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
        final int r = (int) ((colour >> 16 & 255) * f), g = (int) ((colour >> 8 & 255) * f), b = (int) ((colour & 255) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
