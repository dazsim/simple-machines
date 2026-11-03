package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.gen.TurbineBladeBlock;
import com.workshopcraft.simplemachines.gen.WindTurbineBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws the blades stacked on a turbine base as one helix, turned by the base so they all stay in step. */
public class WindTurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity, WindTurbineRenderer.State> {
    /** Twist of the helix per pixel of height, so each blade turns half a circle and the next carries on. */
    public static final float TWIST = 180F / 16F;

    public static class State extends BlockEntityRenderState {
        int blades;
        float angle;
        final int[] light = new int[TurbineBladeBlock.MAX];
    }

    public WindTurbineRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WindTurbineBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, breaking);
        final Level level = be.getLevel();
        s.blades = 0;
        if (level == null) return;
        spin(be, level.getGameTime() + partialTick);
        WindSound.seen(be);
        final BlockPos pos = be.getBlockPos();
        int n = 0;
        while (n < TurbineBladeBlock.MAX) {
            final var st = level.getBlockState(pos.above(n + 1));
            if (!st.is(SimpleMachines.turbineBlade) || st.getValue(TurbineBladeBlock.STACK) != n + 1) break;
            s.light[n] = LevelRenderer.getLightCoords(level, pos.above(n + 1));
            n++;
        }
        s.blades = n;
        s.angle = be.angle;
    }

    /** Moves the helix on by the time since the last frame, easing the speed towards what the output asks for. */
    static void spin(WindTurbineBlockEntity be, float time) {
        final long now = (long) (time * 1000);
        if (be.lastFrame < 0) {
            be.lastFrame = now;
            be.speed = be.targetSpeed();
            return;
        }
        final float dt = Math.min(20F, (now - be.lastFrame) / 1000F);
        if (dt <= 0) return;
        be.lastFrame = now;
        be.speed += (be.targetSpeed() - be.speed) * Math.min(1F, dt * 0.08F);
        if (be.speed < 0.02F && be.targetSpeed() == 0) be.speed = 0;
        be.angle = (be.angle + be.speed * dt) % 360F;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.blades == 0) return;
        final TextureAtlasSprite blade = WorktopRenderer.get(SimpleMachines.id("block/wind_turbine_blade"));
        final TextureAtlasSprite post = WorktopRenderer.get(SimpleMachines.id("block/wind_turbine_post"));
        final TextureAtlasSprite bolt = WorktopRenderer.get(net.minecraft.resources.Identifier.withDefaultNamespace("block/copper_block"));
        out.submitCustomGeometry(pose, Sheets.cutoutBlockSheet(), (p, vc) -> {
            for (int k = 0; k < s.blades; k++) {
                final Quads light = new Quads(p, vc, s.light[k], OverlayTexture.NO_OVERLAY);
                final float y = 16 * (k + 1);
                // the shaft, joined to the post on the base
                box(light, 7.5F, y, 7.5F, 8.5F, y + 16, 8.5F, post, 0, 16);
                for (int j = 0; j < 16; j++) {
                    final PoseStack ps = new PoseStack();
                    ps.last().set(p);
                    ps.translate(0.5F, 0F, 0.5F);
                    ps.mulPose(Axis.YP.rotationDegrees(s.angle + (k * 16 + j) * TWIST));
                    final Quads q = new Quads(ps.last(), vc, s.light[k], OverlayTexture.NO_OVERLAY);
                    box(q, -7.5F, y + j, -1F, 7.5F, y + j + 1, 1F, blade, 15 - j, 16 - j);
                }
            }
            final Quads top = new Quads(p, vc, s.light[s.blades - 1], OverlayTexture.NO_OVERLAY);
            final float y = 16 * (s.blades + 1);
            box(top, 5.5F, y - 0.5F, 5.5F, 10.5F, y, 10.5F, bolt, 0, 0.5F);
        });
    }

    private static void box(Quads q, float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sp, float v0, float v1) {
        final float w = x1 - x0;
        final float d = z1 - z0;
        final float u0 = 8 - w / 2, u1 = 8 + w / 2;
        q.face(Direction.UP, x0, y0, z0, x1, y1, z1, sp, u0, 8 - d / 2, u1, 8 + d / 2);
        q.face(Direction.DOWN, x0, y0, z0, x1, y1, z1, sp, u0, 8 - d / 2, u1, 8 + d / 2);
        q.face(Direction.NORTH, x0, y0, z0, x1, y1, z1, sp, u0, v0, u1, v1);
        q.face(Direction.SOUTH, x0, y0, z0, x1, y1, z1, sp, u0, v0, u1, v1);
        q.face(Direction.EAST, x0, y0, z0, x1, y1, z1, sp, 8 - d / 2, v0, 8 + d / 2, v1);
        q.face(Direction.WEST, x0, y0, z0, x1, y1, z1, sp, 8 - d / 2, v0, 8 + d / 2, v1);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    // NeoForge asks the renderer for the box it culls against
    public AABB getRenderBoundingBox(WindTurbineBlockEntity be) {
        final BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2 + TurbineBladeBlock.MAX, pos.getZ() + 1);
    }
}
