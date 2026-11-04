package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Draws straight into the buffers, standing in for the newer deferred submit collector. */
public final class Submit {
    public interface Geometry {
        void draw(PoseStack.Pose pose, VertexConsumer vc);
    }

    public record Crumbling(int progress, PoseStack.Pose cameraPose) {
    }

    /** The render state base: what the renderers read from it. */
    public static class State {
        public int lightCoords;
        public Crumbling breakProgress;
    }

    public final MultiBufferSource buffers;

    public Submit(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public void submitCustomGeometry(PoseStack pose, RenderType type, Geometry g) {
        g.draw(pose.last(), buffers.getBuffer(type));
    }
}
