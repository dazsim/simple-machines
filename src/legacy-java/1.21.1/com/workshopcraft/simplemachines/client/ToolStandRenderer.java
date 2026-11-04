package com.workshopcraft.simplemachines.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.ToolStandBlock;
import com.workshopcraft.simplemachines.assembly.ToolStandBlockEntity;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Draws the tool on a Tool Stand with only the parts fitted so far; a loose part floats just above its place. */
public class ToolStandRenderer implements BlockEntityRenderer<ToolStandBlockEntity> {
    public static final int FRAME = 1, CORE = 2, BOARD = 4, BATTERY = 8, UPGRADES = 16, TIP = 32;

    static class State {
        Direction facing = Direction.NORTH;
        ItemStack fixed = ItemStack.EMPTY;
        ItemStack loose = ItemStack.EMPTY;
        Level level;
        int seed;
    }

    public ToolStandRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ToolStandBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        final State s = new State();
        fill(be, s);
        draw(s, s.fixed, s.seed, 0F, pose, buffers, light);
        draw(s, s.loose, s.seed + 1, 0.12F, pose, buffers, light);
    }

    private static void fill(ToolStandBlockEntity be, State s) {
        s.facing = be.getBlockState().getValue(ToolStandBlock.FACING);
        s.fixed = ItemStack.EMPTY;
        s.loose = ItemStack.EMPTY;
        s.level = be.getLevel();
        s.seed = (int) be.getBlockPos().asLong();
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
        if (fixedParts != 0) s.fixed = fixed;
        if (be.tipLoose()) {
            final ItemStack loose = base.copy();
            loose.set(SimpleMachines.TOOL_TIP, "diamond");
            loose.set(SimpleMachines.TOOL_PARTS, TIP);
            s.loose = loose;
        } else if (loosePart != 0 || be.loose() != null) {
            final ItemStack loose = base.copy();
            if (be.loose() != null) loose.set(SimpleMachines.UPGRADES_FITTED, be.loose().id);
            loose.set(SimpleMachines.TOOL_PARTS, be.loose() != null ? UPGRADES : loosePart);
            s.loose = loose;
        }
    }

    // the tool lies along the stand's long side, nose to the right as you face it
    private static void draw(State s, ItemStack item, int seed, float lift, PoseStack pose, MultiBufferSource buffers, int light) {
        if (item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5F, ToolStandBlock.TOP + lift, 0.5F);
        RenderCompat.rotate(pose, Axis.YP.rotationDegrees(180F - s.facing.toYRot()));
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, s.level, seed);
        pose.popPose();
    }
}
