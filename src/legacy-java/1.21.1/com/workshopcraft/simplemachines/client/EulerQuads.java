package com.workshopcraft.simplemachines.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.workshopcraft.simplemachines.SimpleMachines;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Applies the 26.x Euler element rotations that 1.21.1 model json cannot express, keyed by the tint codes res1211.py writes. */
public final class EulerQuads {
    private static Map<Integer, float[]> table = Map.of();

    public static void load(ResourceManager rm) {
        Map<Integer, float[]> t = new HashMap<>();
        rm.getResource(SimpleMachines.id("euler_rotations.json")).ifPresent(r -> {
            try (Reader in = r.openAsReader()) {
                JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                for (var e : o.entrySet()) {
                    var a = e.getValue().getAsJsonArray();
                    float[] v = new float[6];
                    for (int i = 0; i < 6; i++) v[i] = a.get(i).getAsFloat();
                    t.put(Integer.parseInt(e.getKey()), v);
                }
            } catch (Exception ignored) {
            }
        });
        table = t;
    }

    public static BakedModel fix(BakedModel model) {
        if (table.isEmpty() || model == null) return model;
        RandomSource rand = RandomSource.create(42L);
        fix(model.getQuads(null, null, rand));
        for (Direction d : Direction.values()) fix(model.getQuads(null, d, rand));
        return model;
    }

    private static void fix(List<BakedQuad> quads) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad q = quads.get(i);
            float[] r = table.get(q.getTintIndex());
            if (r == null) continue;
            try {
                quads.set(i, rotate(q, r));
            } catch (UnsupportedOperationException ex) {
                return;
            }
        }
    }

    private static BakedQuad rotate(BakedQuad q, float[] r) {
        float k = (float) (Math.PI / 180);
        Matrix4f m = new Matrix4f().rotationZYX(r[2] * k, r[1] * k, r[0] * k);
        Vector3f o = new Vector3f(r[3], r[4], r[5]).div(16);
        int[] v = q.getVertices().clone();
        int stride = v.length / 4;
        Vector3f p = new Vector3f();
        for (int i = 0; i < 4; i++) {
            int b = i * stride;
            p.set(Float.intBitsToFloat(v[b]), Float.intBitsToFloat(v[b + 1]), Float.intBitsToFloat(v[b + 2])).sub(o);
            m.transformPosition(p).add(o);
            v[b] = Float.floatToRawIntBits(p.x);
            v[b + 1] = Float.floatToRawIntBits(p.y);
            v[b + 2] = Float.floatToRawIntBits(p.z);
        }
        Vector3f n = new Vector3f(q.getDirection().step());
        m.transformDirection(n);
        if (stride >= 8) {
            int packed = v[7] & 0xFF000000;
            int nx = ((int) (n.x * 127)) & 0xFF, ny = ((int) (n.y * 127)) & 0xFF, nz = ((int) (n.z * 127)) & 0xFF;
            for (int i = 0; i < 4; i++) v[i * stride + 7] = packed | nx | ny << 8 | nz << 16;
        }
        Direction d = Direction.getNearest(n.x, n.y, n.z);
        return new BakedQuad(v, -1, d, q.getSprite(), q.isShade());
    }
}
