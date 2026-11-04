package com.workshopcraft.simplemachines.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.ParticleRenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * Hot steel spark drawn as a streak along its velocity: glows yellow-white to orange to dull red, falls, and a few bounce.
 * The white-hot kind (a bit too soft for the block) stays white to pale yellow.
 */
public class HotSparkParticle extends TextureSheetParticle {
    private static final int MAX_SEGMENTS = 14;
    private final boolean white;
    private int bounces;
    private final double x0, y0, z0;

    HotSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprite, boolean white) {
        super(level, x, y, z);
        pickSprite(sprite);
        this.white = white;
        x0 = x;
        y0 = y;
        z0 = z;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        hasPhysics = false;
        gravity = 0.9F;
        lifetime = 14 + random.nextInt(14);
        quadSize = 0.035F + random.nextFloat() * 0.03F;
        bounces = random.nextInt(3) == 0 ? 1 + random.nextInt(2) : 0;
        colour();
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        yd -= 0.04 * gravity;
        double nx = x + xd, ny = y + yd, nz = z + zd;
        if (solid(nx, y, nz)) {
            xd *= -0.3;
            zd *= -0.3;
            nx = x;
            nz = z;
        }
        if (yd < 0 && solid(nx, ny, nz)) {
            if (bounces-- > 0 && yd < -0.03) {
                yd = -yd * 0.45;
                xd *= 0.6;
                zd *= 0.6;
            } else {
                xd *= 0.3;
                zd *= 0.3;
                yd = 0;
            }
            ny = y;
        }
        setPos(nx, ny, nz);
        xd *= 0.96;
        yd *= 0.98;
        zd *= 0.96;
        colour();
    }

    private boolean solid(double x, double y, double z) {
        final BlockPos pos = BlockPos.containing(x, y, z);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private void colour() {
        final float t = Mth.clamp((float) age / lifetime, 0F, 1F);
        if (white) {
            setColor(1F, Mth.lerp(t, 1F, 0.9F), Mth.lerp(t, 1F, 0.6F));
            return;
        }
        if (t < 0.35F) {
            final float k = t / 0.35F;
            setColor(1F, Mth.lerp(k, 0.95F, 0.62F), Mth.lerp(k, 0.55F, 0.12F));
        } else {
            final float k = (t - 0.35F) / 0.65F;
            setColor(Mth.lerp(k, 1F, 0.75F), Mth.lerp(k, 0.62F, 0.22F), Mth.lerp(k, 0.12F, 0.04F));
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        return quadSize * (1F - 0.5F * Mth.clamp((age + partialTick) / lifetime, 0F, 1F));
    }

    /** A tapering line of quads back along the motion, so fast sparks read as streaks. */
    @Override
    public void render(VertexConsumer vc, net.minecraft.client.Camera camera, float partialTick) {
        final net.minecraft.world.phys.Vec3 c = camera.getPosition();
        final float x = (float) (Mth.lerp(partialTick, xo, this.x) - c.x());
        final float y = (float) (Mth.lerp(partialTick, yo, this.y) - c.y());
        final float z = (float) (Mth.lerp(partialTick, zo, this.z) - c.z());
        final Quaternionf q = new Quaternionf(camera.rotation());
        if (roll != 0F) q.rotateZ(Mth.lerp(partialTick, oRoll, roll));
        renderRotatedQuad(vc, q, x, y, z, partialTick);
    }

    private void renderRotatedQuad(VertexConsumer state, Quaternionf rotation, float x, float y, float z, float partialTick) {
        final float size = getQuadSize(partialTick);
        float tx = (float) -xd * 2.6F, ty = (float) -yd * 2.6F, tz = (float) -zd * 2.6F;
        float len = Mth.sqrt(tx * tx + ty * ty + tz * tz);
        // the tail never reaches back past where the spark started
        final double px = Mth.lerp(partialTick, xo, this.x) - x0, py = Mth.lerp(partialTick, yo, this.y) - y0, pz = Mth.lerp(partialTick, zo, this.z) - z0;
        final float flown = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (len > flown && len > 0) {
            final float k = flown / len;
            tx *= k;
            ty *= k;
            tz *= k;
            len = flown;
        }
        final int n = Math.min(MAX_SEGMENTS, 1 + (int) (len / (size * 0.6F)));
        final int light = getLightColor(partialTick);
        for (int i = 0; i < n; i++) {
            final float k = n == 1 ? 0F : (float) i / (n - 1);
            final float fade = 1F - 0.55F * k;
            final float r = rCol * (1F - 0.15F * k);
            final float g = gCol * (1F - (white ? 0.08F : 0.45F) * k), b = bCol * (1F - (white ? 0.2F : 0.6F) * k);
            final float cx = x + tx * k, cy = y + ty * k, cz = z + tz * k, sz = size * fade;
            vertex(state, rotation, cx, cy, cz, 1F, -1F, sz, getU1(), getV1(), r, g, b, light);
            vertex(state, rotation, cx, cy, cz, 1F, 1F, sz, getU1(), getV0(), r, g, b, light);
            vertex(state, rotation, cx, cy, cz, -1F, 1F, sz, getU0(), getV0(), r, g, b, light);
            vertex(state, rotation, cx, cy, cz, -1F, -1F, sz, getU0(), getV1(), r, g, b, light);
        }
    }

    private void vertex(VertexConsumer out, Quaternionf q, float x, float y, float z, float dx, float dy, float size, float u, float v,
                        float r, float g, float b, int light) {
        final Vector3f p = new Vector3f(dx, dy, 0F).rotate(q).mul(size).add(x, y, z);
        out.vertex(p.x(), p.y(), p.z()).uv(u, v).color(r, g, b, alpha).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> new HotSparkParticle(level, x, y, z, xd, yd, zd, sprites, false);
    }

    public static ParticleProvider<SimpleParticleType> whiteProvider(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> new HotSparkParticle(level, x, y, z, xd, yd, zd, sprites, true);
    }
}
