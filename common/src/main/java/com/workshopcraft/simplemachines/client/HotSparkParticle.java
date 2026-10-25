package com.workshopcraft.simplemachines.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Hot steel spark: glows yellow-white to orange to dull red, falls, and a few bounce off the ground. */
public class HotSparkParticle extends SingleQuadParticle {
    private int bounces;

    HotSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        hasPhysics = false;
        gravity = 0.9F;
        lifetime = 8 + random.nextInt(10);
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
        return quadSize * (1F - 0.6F * Mth.clamp((age + partialTick) / lifetime, 0F, 1F));
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.FULL_BRIGHT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd, random) -> new HotSparkParticle(level, x, y, z, xd, yd, zd, sprites.get(random));
    }
}
