package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// cuboid faces in pixel units, uv given as seen from outside the face
public record Quads(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
    public void face(Direction dir, float x0, float y0, float z0, float x1, float y1, float z1,
                     TextureAtlasSprite sp, float u0, float v0, float u1, float v1) {
        face(dir, x0, y0, z0, x1, y1, z1, sp, u0, v0, u1, v1, 0xFFFFFFFF);
    }

    public void face(Direction dir, float x0, float y0, float z0, float x1, float y1, float z1,
                     TextureAtlasSprite sp, float u0, float v0, float u1, float v1, int colour) {
        x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
        float[][] c = switch (dir) {
            case NORTH -> new float[][]{{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}};
            case SOUTH -> new float[][]{{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}};
            case WEST -> new float[][]{{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}};
            case EAST -> new float[][]{{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}};
            case UP -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case DOWN -> new float[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
        };
        float[][] uv = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        Vector3f n = pose.transformNormal(dir.getUnitVec3f(), new Vector3f());
        Matrix4f m = pose.pose();
        Vector3f pos = new Vector3f();
        for (int i = 0; i < 4; i++) {
            m.transformPosition(c[i][0], c[i][1], c[i][2], pos);
            vc.addVertex(pos.x(), pos.y(), pos.z(), colour, sp.getU(uv[i][0] / 16F), sp.getV(uv[i][1] / 16F),
                    overlay, light, n.x(), n.y(), n.z());
        }
    }
}
