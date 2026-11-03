package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** The fluid inside the tank, the clipped cube shell cut off at the fluid level. */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity, TankRenderer.State> {
    // edge bevel and corner cut in pixels, matching the block model
    private static final float E = 2F, K = 4F;
    private static final float INSET = 0.3F, LO = INSET, HI = 16 - INSET;
    private static final float[][] SHELL = shell();

    public static class State extends BlockEntityRenderState {
        @Nullable TextureAtlasSprite sprite;
        int tint;
        float top;
        boolean glow;
    }

    public TankRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    // planes n.p <= d around the block centre, in pixels
    private static float[][] shell() {
        final List<float[]> out = new ArrayList<>();
        final float h = 8 - INSET, k = 1F / (float) Math.sqrt(3);
        for (int a = 0; a < 3; a++) {
            for (int sgn = -1; sgn <= 1; sgn += 2) {
                final float[] p = new float[4];
                p[a] = sgn;
                p[3] = h;
                out.add(p);
            }
        }
        final float r = 1F / (float) Math.sqrt(2);
        if (E > 0)
            for (int a = 0; a < 3; a++)
                for (int sa = -1; sa <= 1; sa += 2)
                    for (int sb = -1; sb <= 1; sb += 2) {
                        final float[] p = new float[4];
                        p[a] = sa * r;
                        p[(a + 1) % 3] = sb * r;
                        p[3] = (16 - E) * r - INSET;
                        out.add(p);
                    }
        for (int x = -1; x <= 1; x += 2)
            for (int y = -1; y <= 1; y += 2)
                for (int z = -1; z <= 1; z += 2) out.add(new float[]{x * k, y * k, z * k, (24 - K) * k - INSET});
        return out.toArray(new float[0][]);
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
        s.top = top(be.amount());
        s.glow = f.defaultFluidState().createLegacyBlock().getLightEmission() > 0;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        if (s.sprite == null) return;
        submitFluid(pose, out, s.sprite, s.tint, s.top, s.glow ? LightCoordsUtil.FULL_BRIGHT : s.lightCoords);
    }

    /** Fill height in pixels for an amount in droplets. */
    public static float top(long amount) {
        return LO + Math.max(0.25F, (HI - LO) * amount / (float) TankBlockEntity.CAPACITY);
    }

    public static void submitFluid(PoseStack pose, SubmitNodeCollector out, TextureAtlasSprite sp, int tint, float top, int light) {
        final float[][] planes = new float[SHELL.length + 1][];
        System.arraycopy(SHELL, 0, planes, 0, SHELL.length);
        planes[SHELL.length] = new float[]{0, 1, 0, top - 8};
        out.submitCustomGeometry(pose, Sheets.translucentBlockSheet(), (p, vc) -> {
            for (int i = 0; i < planes.length; i++) {
                final List<float[]> poly = face(planes, i);
                if (poly.size() >= 3) emit(p, vc, planes[i], poly, sp, tint, light);
            }
        });
    }

    // the face on plane i: a large square on it clipped by every other plane
    private static List<float[]> face(float[][] planes, int i) {
        final float[] n = planes[i];
        final Vector3f nv = new Vector3f(n[0], n[1], n[2]);
        final Vector3f u = Math.abs(n[1]) < 0.9F ? new Vector3f(0, 1, 0).cross(nv).normalize() : new Vector3f(1, 0, 0).cross(nv).normalize();
        final Vector3f v = new Vector3f(nv).cross(u);
        final Vector3f c = new Vector3f(nv).mul(n[3]);
        List<float[]> poly = new ArrayList<>();
        final float r = 40;
        final float[][] sq = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (float[] q : sq)
            poly.add(new float[]{c.x + (u.x * q[0] + v.x * q[1]) * r, c.y + (u.y * q[0] + v.y * q[1]) * r, c.z + (u.z * q[0] + v.z * q[1]) * r});
        for (int j = 0; j < planes.length && poly.size() >= 3; j++) {
            if (j == i) continue;
            final float[] pl = planes[j];
            final List<float[]> next = new ArrayList<>();
            for (int k = 0; k < poly.size(); k++) {
                final float[] a = poly.get(k), b = poly.get((k + 1) % poly.size());
                final float da = a[0] * pl[0] + a[1] * pl[1] + a[2] * pl[2] - pl[3];
                final float db = b[0] * pl[0] + b[1] * pl[1] + b[2] * pl[2] - pl[3];
                if (da <= 0) next.add(a);
                if ((da <= 0) != (db <= 0)) {
                    final float t = da / (da - db);
                    next.add(new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t});
                }
            }
            poly = next;
        }
        return poly;
    }

    private static void emit(PoseStack.Pose p, VertexConsumer vc, float[] n, List<float[]> poly, TextureAtlasSprite sp, int tint, int light) {
        final Vector3f nn = p.transformNormal(n[0], n[1], n[2], new Vector3f());
        final Matrix4f m = p.pose();
        final float ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
        final Vector3f pos = new Vector3f();
        for (int k = 1; k + 1 < poly.size(); k++) {
            final float[][] quad = {poly.get(0), poly.get(k), poly.get(k + 1), poly.get(k + 1)};
            for (float[] q : quad) {
                final float x = q[0] + 8, y = q[1] + 8, z = q[2] + 8;
                final float uu, vv;
                if (ay >= ax && ay >= az) { uu = x; vv = z; }
                else if (ax >= az) { uu = z; vv = 16 - y; }
                else { uu = x; vv = 16 - y; }
                m.transformPosition(x / 16, y / 16, z / 16, pos);
                vc.addVertex(pos.x(), pos.y(), pos.z(), tint, sp.getU(uu / 16F), sp.getV(vv / 16F),
                        OverlayTexture.NO_OVERLAY, light, nn.x(), nn.y(), nn.z());
            }
        }
    }
}
