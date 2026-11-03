package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.FluidPipeBlock;
import com.workshopcraft.simplemachines.pipe.ItemPipeBlock;
import com.workshopcraft.simplemachines.pipe.Link;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;
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

/** Dye sleeves and bands, pipe filters, and the fluid seen through a fluid pipe's window. */
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, ConduitRenderer.State> {
    private static final float GROW = 0.3F;

    public static class State extends BlockEntityRenderState {
        int colour;
        int arms;
        float core;
        float arm;
        int filters;
        boolean copper;
        @Nullable TextureAtlasSprite fluid;
        int fluidTint;
    }

    // texels of the fluid pipe textures that are glass with fluid behind it
    private static final int[][] PIPE_WINDOW, CORE_WINDOW = {{7, 8}, {8, 8}};
    static {
        final java.util.List<int[]> w = new java.util.ArrayList<>();
        for (int i = 2; i <= 13; i++) {
            if (i == 7 || i == 8) continue;
            w.add(new int[]{i, 8});
            w.add(new int[]{8, i});
        }
        PIPE_WINDOW = w.toArray(new int[0][]);
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
        s.copper = state.getBlock() instanceof FluidPipeBlock;
        s.filters = 0;
        if (!cable) for (Direction d : Direction.values()) {
            if (be.hasFilter(d) && ItemPipeBlock.link(state, d) == Link.LINK) s.filters |= 1 << d.get3DDataValue();
        }
        s.fluid = null;
        final Level level = be.getLevel();
        if (s.copper && level != null) {
            final Fluid f = be.shownFluid(level.getGameTime());
            if (f != Fluids.EMPTY) {
                s.fluid = fluidSprite(f);
                s.fluidTint = Minecraft.getInstance().level == null ? 0xFFFFFFFF : fluidTint(f, Minecraft.getInstance().level, be.getBlockPos());
            }
        }
    }

    public static TextureAtlasSprite fluidSprite(Fluid f) {
        return model(f).stillMaterial().sprite();
    }

    public static int fluidTint(Fluid f, net.minecraft.client.renderer.block.BlockAndTintGetter level, BlockPos pos) {
        final BlockTintSource tint = model(f).tintSource();
        return tint == null ? 0xFFFFFFFF : tint.colorInWorld(f.defaultFluidState().createLegacyBlock(), level, pos) | 0xFF000000;
    }

    private static FluidModel model(Fluid f) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState());
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, SimpleMachines.id("block/" + name)));
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.filters != 0) filters(s, pose, out);
        if (s.fluid != null) window(s, pose, out);
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
                final boolean filtered = (s.filters & 1 << d.get3DDataValue()) != 0;
                final float lo = d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? (filtered ? 10.5F : 13) : (filtered ? 4 : 1);
                if (filtered && !s.copper) continue;
                final float hi = lo + 2;
                switch (d.getAxis()) {
                    case X -> cube(q, sp, s.colour, lo, a0, a0, hi, a1, a1);
                    case Y -> cube(q, sp, s.colour, a0, lo, a0, a1, hi, a1);
                    case Z -> cube(q, sp, s.colour, a0, a0, lo, a1, a1, hi);
                }
            }
        });
    }

    // filter parts, laid out for the north end with z measured in from the block face
    private static final float[][] FILTER = {
            {4.5F, 4.5F, -0.01F, 11.5F, 11.5F, 1},
            {5.95F, 5.95F, 1, 10.05F, 10.05F, 2.5F},
            {5.5F, 7, 1, 6, 9, 2.5F}, {10, 7, 1, 10.5F, 9, 2.5F},
            {6.25F, 6.25F, 2.5F, 9.75F, 9.75F, 4},
            {7.5F, 9.75F, 2.75F, 8.5F, 10.75F, 3.75F},
            {6.75F, 9.75F, 1.5F, 7.25F, 10.25F, 4}, {8.75F, 9.75F, 1.5F, 9.25F, 10.25F, 4},
            {6.75F, 5.75F, 1.5F, 7.25F, 6.25F, 4}, {8.75F, 5.75F, 1.5F, 9.25F, 6.25F, 4},
            {5.75F, 6.75F, 1.5F, 6.25F, 7.25F, 4}, {5.75F, 8.75F, 1.5F, 6.25F, 9.25F, 4},
            {9.75F, 6.75F, 1.5F, 10.25F, 7.25F, 4}, {9.75F, 8.75F, 1.5F, 10.25F, 9.25F, 4},
            {4.75F, 4.75F, 1, 5.75F, 5.75F, 1.5F}, {10.25F, 4.75F, 1, 11.25F, 5.75F, 1.5F},
            {4.75F, 10.25F, 1, 5.75F, 11.25F, 1.5F}, {10.25F, 10.25F, 1, 11.25F, 11.25F, 1.5F},
    };
    // 0 flange, 1 ring, 2 rib, 3 sleeve, 4 lens
    private static final int[] FILTER_TEX = {0, 1, 2, 2, 3, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2};

    private static void filters(State s, PoseStack pose, SubmitNodeCollector out) {
        final String kind = s.copper ? "fluid_pipe" : "item_pipe";
        final TextureAtlasSprite[] tex = {sprite(kind + "_filter"), sprite(kind + "_link"), sprite(kind + "_filter_rib"),
                sprite(kind + "_filter_sleeve"), sprite("item_pipe_filter_lens")};
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> {
            final Quads q = new Quads(p, vc, s.lightCoords, OverlayTexture.NO_OVERLAY);
            for (Direction d : Direction.values()) {
                if ((s.filters & 1 << d.get3DDataValue()) == 0) continue;
                for (int i = 0; i < FILTER.length; i++) {
                    final float[] b = FILTER[i];
                    for (Direction face : Direction.values()) {
                        if (i == 4 && face.getAxis() == Direction.Axis.Z) continue;
                        box(q, tex[FILTER_TEX[i]], 0xFFFFFFFF, d, face, b[0], b[1], b[2], b[3], b[4], b[5], null);
                    }
                }
            }
        });
    }

    private static void window(State s, PoseStack pose, SubmitNodeCollector out) {
        out.submitCustomGeometry(pose, Sheets.translucentBlockSheet(), (p, vc) -> {
            final Quads q = new Quads(p, vc, s.lightCoords, OverlayTexture.NO_OVERLAY);
            for (Direction face : Direction.values()) {
                if ((s.arms & 1 << face.get3DDataValue()) != 0) continue;
                box(q, s.fluid, s.fluidTint, Direction.NORTH, face, 5.5F, 5.5F, 5.5F, 10.5F, 10.5F, 10.5F, CORE_WINDOW);
            }
            for (Direction d : Direction.values()) {
                if ((s.arms & 1 << d.get3DDataValue()) == 0) continue;
                for (Direction face : new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}) {
                    box(q, s.fluid, s.fluidTint, d, face, 6.5F, 6.5F, 0, 9.5F, 9.5F, 5.5F, PIPE_WINDOW);
                }
            }
        });
    }

    /**
     * Draws one face of a box given in north-end space, turned to the end at side the way the block model turns it.
     * With texels set, only those texels of the face's texture are drawn, just outside the face, using sp for each.
     */
    private static void box(Quads q, TextureAtlasSprite sp, int colour, Direction side, Direction face,
                            float x0, float y0, float z0, float x1, float y1, float z1, int @Nullable [][] texels) {
        final float u0, v0, u1, v1;
        switch (face) {
            case NORTH -> { u0 = 16 - x1; v0 = 16 - y1; u1 = 16 - x0; v1 = 16 - y0; }
            case SOUTH -> { u0 = x0; v0 = 16 - y1; u1 = x1; v1 = 16 - y0; }
            case EAST -> { u0 = 16 - z1; v0 = 16 - y1; u1 = 16 - z0; v1 = 16 - y0; }
            case WEST -> { u0 = z0; v0 = 16 - y1; u1 = z1; v1 = 16 - y0; }
            case UP -> { u0 = x0; v0 = z0; u1 = x1; v1 = z1; }
            default -> { u0 = x0; v0 = 16 - z1; u1 = x1; v1 = 16 - z0; }
        }
        if (texels == null) {
            emit(q, sp, colour, side, face, x0, y0, z0, x1, y1, z1, u0, v0, u1, v1);
            return;
        }
        final float e = 0.02F;
        for (int[] t : texels) {
            final float a0 = Math.max(u0, t[0]), a1 = Math.min(u1, t[0] + 1), b0 = Math.max(v0, t[1]), b1 = Math.min(v1, t[1] + 1);
            if (a0 >= a1 || b0 >= b1) continue;
            float px0 = x0, py0 = y0, pz0 = z0, px1 = x1, py1 = y1, pz1 = z1;
            switch (face) {
                case NORTH -> { px0 = 16 - a1; px1 = 16 - a0; py0 = 16 - b1; py1 = 16 - b0; pz0 = pz1 = z0 - e; }
                case SOUTH -> { px0 = a0; px1 = a1; py0 = 16 - b1; py1 = 16 - b0; pz0 = pz1 = z1 + e; }
                case EAST -> { pz0 = 16 - a1; pz1 = 16 - a0; py0 = 16 - b1; py1 = 16 - b0; px0 = px1 = x1 + e; }
                case WEST -> { pz0 = a0; pz1 = a1; py0 = 16 - b1; py1 = 16 - b0; px0 = px1 = x0 - e; }
                case UP -> { px0 = a0; px1 = a1; pz0 = b0; pz1 = b1; py0 = py1 = y1 + e; }
                default -> { px0 = a0; px1 = a1; pz0 = 16 - b1; pz1 = 16 - b0; py0 = py1 = y0 - e; }
            }
            emit(q, sp, colour, side, face, px0, py0, pz0, px1, py1, pz1, a0, b0, a1, b1);
        }
    }

    private static void emit(Quads q, TextureAtlasSprite sp, int colour, Direction side, Direction face,
                             float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1) {
        final float[] a = turn(side, x0, y0, z0), b = turn(side, x1, y1, z1);
        q.face(turn(side, face), Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]), sp, u0, v0, u1, v1, shade(colour, turn(side, face)));
    }

    // same turns as the blockstate: east y90, south y180, west y270, up x270, down x90
    private static float[] turn(Direction side, float x, float y, float z) {
        return switch (side) {
            case NORTH -> new float[]{x, y, z};
            case EAST -> new float[]{16 - z, y, x};
            case SOUTH -> new float[]{16 - x, y, 16 - z};
            case WEST -> new float[]{z, y, 16 - x};
            case UP -> new float[]{x, 16 - z, y};
            case DOWN -> new float[]{x, z, 16 - y};
        };
    }

    private static Direction turn(Direction side, Direction face) {
        final float[] v = turn(side, 8 + 8 * face.getStepX(), 8 + 8 * face.getStepY(), 8 + 8 * face.getStepZ());
        return Direction.getApproximateNearest(v[0] - 8, v[1] - 8, v[2] - 8);
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
