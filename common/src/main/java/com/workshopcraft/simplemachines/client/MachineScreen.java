package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final Identifier LIT = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private final Identifier texture;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        texture = SimpleMachines.id(menu.inputs == 2 ? "textures/gui/alloy_furnace.png" : "textures/gui/extruder.png");
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            final int h = Mth.ceil(menu.litProgress() * 13) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
        }
        final int w = Mth.ceil(menu.cookProgress() * 24);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN, 24, 16, 0, 0, x + 79, y + 34, w, 16);
    }
}
