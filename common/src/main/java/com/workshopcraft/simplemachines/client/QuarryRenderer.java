package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.MachineBlock;
import com.workshopcraft.simplemachines.quarry.QuarryBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The quarry's worktop plus its gantry, carriage, shaft and drill head, moved smoothly between ticks. */
public class QuarryRenderer extends WorktopRenderer<QuarryBlockEntity> {
    public static class QState extends WorktopRenderer.State {
        boolean show;
        Direction facing = Direction.NORTH;
        boolean alongX;
        float railFrom;
        float railTo;
        float tx;
        float ty;
        float tz;
        float gantryY;
        int railLight;
        int headLight;
    }

    public QuarryRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public State createRenderState() {
        return new QState();
    }

    @Override
    public void extractRenderState(QuarryBlockEntity be, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        super.extractRenderState(be, state, partialTick, camera, breaking);
        final QState s = (QState) state;
        final Level level = be.getLevel();
        s.show = false;
        if (level == null || !be.getBlockState().hasProperty(MachineBlock.FACING)) return;
        final double[] tip = be.tip(level.getGameTime() + partialTick);
        if (tip == null) return;
        final BlockPos pos = be.getBlockPos();
        final Direction facing = be.getBlockState().getValue(MachineBlock.FACING);
        s.show = true;
        s.facing = facing;
        s.alongX = facing.getAxis() == Direction.Axis.X;
        final BlockPos far = pos.relative(facing, be.length());
        s.railFrom = s.alongX ? Math.min(0, far.getX() - pos.getX()) : Math.min(0, far.getZ() - pos.getZ());
        s.railTo = s.alongX ? Math.max(0, far.getX() - pos.getX()) : Math.max(0, far.getZ() - pos.getZ());
        s.tx = (float) (tip[0] - pos.getX());
        s.ty = (float) (tip[1] - pos.getY());
        s.tz = (float) (tip[2] - pos.getZ());
        s.gantryY = 2;
        s.railLight = LevelRenderer.getLightCoords(level, BlockPos.containing(tip[0], pos.getY() + 3, tip[2]));
        s.headLight = LevelRenderer.getLightCoords(level, BlockPos.containing(tip[0], tip[1] + 0.3, tip[2]));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        super.submit(state, pose, out, camera);
        final QState s = (QState) state;
        if (!s.show) return;
        final TextureAtlasSprite rail = get(tex("quarry_rail"));
        final TextureAtlasSprite steel = get(tex("quarry_post"));
        final TextureAtlasSprite carriage = get(tex("quarry_carriage"));
        final TextureAtlasSprite front = get(tex("quarry_head_front"));
        final TextureAtlasSprite bit = get(tex("quarry_bit_face"));
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> {
            final Quads r = new Quads(p, vc, s.railLight, OverlayTexture.NO_OVERLAY);
            final float y0 = (s.gantryY + 11F / 16F) * 16F;
            final float y1 = y0 + 6F;
            // gantry rail, one block a segment so the stripes keep their size
            for (int k = (int) s.railFrom; k < (int) s.railTo; k++) {
                final float a0 = (k + 0.5F) * 16F;
                final float a1 = a0 + 16F;
                if (s.alongX) {
                    final float c = s.tz * 16F;
                    railX(r, a0, y0, c - 2.5F, a1, y1, c + 2.5F, rail);
                } else {
                    final float c = s.tx * 16F;
                    railZ(r, c - 2.5F, y0, a0, c + 2.5F, y1, a1, rail);
                }
            }
            final float hx = s.tx * 16F;
            final float hz = s.tz * 16F;
            box(r, hx - 5, y0 - 2, hz - 5, hx + 5, y1 + 2, hz + 5, steel, carriage, steel);
            final Quads h = new Quads(p, vc, s.headLight, OverlayTexture.NO_OVERLAY);
            final float tip = s.ty * 16F;
            // shaft
            box(h, hx - 1.25F, tip + 9, hz - 1.25F, hx + 1.25F, y0 - 2, hz + 1.25F, steel, steel, steel);
            // body with the drill face towards the quarry's front
            head(h, hx - 5, tip + 3, hz - 5, hx + 5, tip + 9, hz + 5, steel, front, s.facing);
            box(h, hx - 3, tip, hz - 3, hx + 3, tip + 3, hz + 3, bit, steel, bit);
        });
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(SimpleMachines.MODID, "block/" + name);
    }

    private static void railX(Quads q, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sp) {
        q.face(Direction.UP, x0, y0, z0, x1, y1, z1, sp, 0, 5.5F, 16, 10.5F);
        q.face(Direction.DOWN, x0, y0, z0, x1, y1, z1, sp, 0, 5.5F, 16, 10.5F);
        q.face(Direction.NORTH, x0, y0, z0, x1, y1, z1, sp, 0, 5, 16, 11);
        q.face(Direction.SOUTH, x0, y0, z0, x1, y1, z1, sp, 0, 5, 16, 11);
    }

    private static void railZ(Quads q, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sp) {
        // the stripes run along u, so the top and bottom are drawn turned a quarter
        q.face(Direction.WEST, x0, y0, z0, x1, y1, z1, sp, 0, 5, 16, 11);
        q.face(Direction.EAST, x0, y0, z0, x1, y1, z1, sp, 0, 5, 16, 11);
        turned(q, Direction.UP, x0, y1, z0, x1, z1, sp);
        turned(q, Direction.DOWN, x0, y0, z0, x1, z1, sp);
    }

    private static void turned(Quads q, Direction d, float x0, float y, float z0, float x1, float z1, TextureAtlasSprite sp) {
        final PoseStack ps = new PoseStack();
        ps.last().set(q.pose());
        ps.translate((x0 + x1) / 32F, y / 16F, (z0 + z1) / 32F);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        final float hx = (z1 - z0) / 2F;
        final float hz = (x1 - x0) / 2F;
        final Quads t = new Quads(ps.last(), q.vc(), q.light(), q.overlay());
        t.face(d, -hx, 0, -hz, hx, 0, hz, sp, 0, 5.5F, 16, 10.5F);
    }

    private static void box(Quads q, float x0, float y0, float z0, float x1, float y1, float z1,
                            TextureAtlasSprite side, TextureAtlasSprite top, TextureAtlasSprite bottom) {
        final float w = Math.min(16, x1 - x0);
        final float d = Math.min(16, z1 - z0);
        final float h = Math.min(16, y1 - y0);
        q.face(Direction.UP, x0, y0, z0, x1, y1, z1, top, 8 - w / 2, 8 - d / 2, 8 + w / 2, 8 + d / 2);
        q.face(Direction.DOWN, x0, y0, z0, x1, y1, z1, bottom, 8 - w / 2, 8 - d / 2, 8 + w / 2, 8 + d / 2);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            final float across = dir.getAxis() == Direction.Axis.X ? d : w;
            q.face(dir, x0, y0, z0, x1, y1, z1, side, 8 - across / 2, 8 - h / 2, 8 + across / 2, 8 + h / 2);
        }
    }

    private static void head(Quads q, float x0, float y0, float z0, float x1, float y1, float z1,
                             TextureAtlasSprite steel, TextureAtlasSprite front, Direction facing) {
        q.face(Direction.UP, x0, y0, z0, x1, y1, z1, steel, 6, 3, 9, 13);
        q.face(Direction.DOWN, x0, y0, z0, x1, y1, z1, steel, 6, 3, 9, 13);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == facing) q.face(dir, x0, y0, z0, x1, y1, z1, front, 3, 5.5F, 13, 11.5F);
            else q.face(dir, x0, y0, z0, x1, y1, z1, steel, 6, 5, 9, 11);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }

    // NeoForge asks the renderer for the box it culls against
    public AABB getRenderBoundingBox(QuarryBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        final int r = QuarryBlockEntity.MAX + 2;
        final int bottom = be.getLevel() == null ? pos.getY() - 400 : be.getLevel().getMinY();
        return new AABB(pos.getX() - r, bottom, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + 4, pos.getZ() + r + 1);
    }
}
