package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.math.Axis;
import com.workshopcraft.simplemachines.assembly.ChassisBlock;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Worktops;
import com.workshopcraft.simplemachines.machine.MachineBlock;
import com.workshopcraft.simplemachines.machine.PoweredMachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.core.Direction.*;

/** Draws the worktop of a chassis or machine from whichever mod it came from, with the core's cut-out. */
public class WorktopRenderer<T extends BlockEntity & Worktops.Holder> implements BlockEntityRenderer<T, WorktopRenderer.State> {
    private static final int SHADE = 0xFFB8B8B8;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        Core core = Core.NONE;
        String worktop = "";
        boolean loose;
        int topLight;
    }

    public WorktopRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, breaking);
        final BlockState state = be.getBlockState();
        s.worktop = be.worktop();
        if (state.getBlock() instanceof ChassisBlock) {
            s.facing = state.getValue(ChassisBlock.FACING);
            s.core = state.getValue(ChassisBlock.CORE);
            s.loose = true;
            if (state.getValue(ChassisBlock.STEP) != 7) s.worktop = "";
        } else if (state.getBlock() instanceof PoweredMachineBlock machine) {
            s.facing = state.getValue(MachineBlock.FACING);
            s.core = machine.core();
            s.loose = false;
        } else {
            s.worktop = "";
        }
        s.topLight = be.getLevel() == null ? s.lightCoords : LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos().above());
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.worktop.isEmpty()) return;
        final TextureAtlasSprite sprite = sprite(s.worktop);
        final float[][] rects = rects(s.core);
        final float lift = s.loose ? 1.5F : 0F;
        pose.pushPose();
        pose.translate(0.5F, lift / 16F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180F - s.facing.toYRot()));
        pose.translate(-0.5F, 0, -0.5F);
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> draw(new Quads(p, vc, s.topLight, OverlayTexture.NO_OVERLAY), rects, sprite));
        if (s.breakProgress != null) {
            final ModelFeatureRenderer.CrumblingOverlay c = s.breakProgress;
            out.submitCustomGeometry(pose, ModelBakery.DESTROY_TYPES.get(c.progress()), (p, vc) ->
                    draw(new Quads(p, new SheetedDecalTextureGenerator(vc, c.cameraPose(), 1F), s.topLight, OverlayTexture.NO_OVERLAY), rects, sprite));
        }
        pose.popPose();
    }

    // same surface as a Much More Storage worktop, minus the opening over the core
    private static void draw(Quads q, float[][] rects, TextureAtlasSprite sp) {
        for (float[] r : rects) {
            final float a0 = r[0], b0 = r[1], a1 = r[2], b1 = r[3];
            q.face(UP, a0, 14, b0, a1, 16, b1, sp, a0, b0, a1, b1);
            q.face(DOWN, a0, 14, b0, a1, 16, b1, sp, a0, b0, a1, b1, SHADE);
            q.face(NORTH, a0, 14, b0, a1, 16, b1, sp, 16 - a1, 0, 16 - a0, 2);
            q.face(SOUTH, a0, 14, b0, a1, 16, b1, sp, a0, 14, a1, 16);
            q.face(EAST, a0, 14, b0, a1, 16, b1, sp, 16 - b1, 0, 16 - b0, 2);
            q.face(WEST, a0, 14, b0, a1, 16, b1, sp, b0, 0, b1, 2);
        }
    }

    private static float[][] rects(Core core) {
        final float[] hole = switch (core) {
            case FURNACE -> new float[]{5, 5, 11, 11};
            case ROLLING -> new float[]{4, 6, 12, 10};
            default -> null;
        };
        if (hole == null) return new float[][]{{0, 0, 16, 16}};
        return new float[][]{{0, 0, 16, hole[1]}, {0, hole[3], 16, 16}, {0, hole[1], hole[0], hole[3]}, {hole[2], hole[1], 16, hole[3]}};
    }

    /** ns:block/path of the worktop item, falling back to the vanilla block it is cut from. */
    public static TextureAtlasSprite sprite(String worktop) {
        Identifier id = Identifier.tryParse(worktop);
        if (id == null) id = Identifier.parse(Worktops.DEFAULT);
        TextureAtlasSprite sp = get(id.withPrefix("block/"));
        if (missing(sp) && id.getPath().endsWith("_worktop")) {
            sp = get(Identifier.withDefaultNamespace("block/" + id.getPath().substring(0, id.getPath().length() - 8)));
        }
        return sp;
    }

    private static TextureAtlasSprite get(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_BLOCKS, id));
    }

    private static boolean missing(TextureAtlasSprite sp) {
        return sp.contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }
}
