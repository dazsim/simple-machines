package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.ToolStandBlock;
import com.workshopcraft.simplemachines.assembly.ToolStandBlockEntity;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Draws the tool on a Tool Stand with only the parts fitted so far; a loose part floats just above its place. */
public class ToolStandRenderer implements BlockEntityRenderer<ToolStandBlockEntity, ToolStandRenderer.State> {
    public static final int FRAME = 1, CORE = 2, BOARD = 4, BATTERY = 8, UPGRADES = 16, TIP = 32;
    private final ItemModelResolver items;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        final ItemStackRenderState fixed = new ItemStackRenderState();
        final ItemStackRenderState loose = new ItemStackRenderState();
    }

    public ToolStandRenderer(BlockEntityRendererProvider.Context ctx) {
        items = ctx.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ToolStandBlockEntity be, State s, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(be, s, partialTick, camera, breaking);
        s.facing = be.getBlockState().getValue(ToolStandBlock.FACING);
        s.fixed.clear();
        s.loose.clear();
        final int step = be.step();
        if (step == 0) return;
        final ItemStack base = be.tool() == null ? new ItemStack(SimpleMachines.TOOLS.get(0)) : be.build();
        base.set(SimpleMachines.TOOL_FRAME, be.frame().isEmpty() ? "iron" : be.frame());
        base.remove(SimpleMachines.UPGRADES_FITTED);
        final int fixedParts = (1 << (step / 2)) - 1;
        final int loosePart = step % 2 == 1 ? 1 << (step / 2) : 0;
        final int seed = (int) be.getBlockPos().asLong();
        final ItemStack fixed = base.copy();
        final java.util.List<Upgrade> ups = new java.util.ArrayList<>(be.upgrades());
        ups.sort(java.util.Comparator.comparingInt(Enum::ordinal));
        if (!ups.isEmpty()) fixed.set(SimpleMachines.UPGRADES_FITTED, Upgrade.join(ups));
        fixed.set(SimpleMachines.TOOL_PARTS, fixedParts | UPGRADES);
        if (be.tipLoose()) fixed.set(SimpleMachines.TOOL_TIP, "none");
        if (fixedParts != 0) items.updateForTopItem(s.fixed, fixed, ItemDisplayContext.FIXED, be.getLevel(), null, seed);
        if (be.tipLoose()) {
            final ItemStack loose = base.copy();
            loose.set(SimpleMachines.TOOL_TIP, "diamond");
            loose.set(SimpleMachines.TOOL_PARTS, TIP);
            items.updateForTopItem(s.loose, loose, ItemDisplayContext.FIXED, be.getLevel(), null, seed + 1);
        } else if (loosePart != 0 || be.loose() != null) {
            final ItemStack loose = base.copy();
            if (be.loose() != null) loose.set(SimpleMachines.UPGRADES_FITTED, be.loose().id);
            loose.set(SimpleMachines.TOOL_PARTS, be.loose() != null ? UPGRADES : loosePart);
            items.updateForTopItem(s.loose, loose, ItemDisplayContext.FIXED, be.getLevel(), null, seed + 1);
        }
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        draw(s, s.fixed, 0F, pose, out);
        draw(s, s.loose, 0.12F, pose, out);
    }

    // the tool lies along the stand's long side, nose to the right as you face it
    private static void draw(State s, ItemStackRenderState item, float lift, PoseStack pose, SubmitNodeCollector out) {
        if (item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5F, ToolStandBlock.TOP + lift, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180F - s.facing.toYRot()));
        item.submit(pose, out, s.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
