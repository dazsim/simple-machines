package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final ResourceLocation LIT = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private final ResourceLocation texture;

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
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            final int h = Mth.ceil(menu.litProgress() * 13) + 1;
            graphics.blitSprite(LIT, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
        }
        final int w = Mth.ceil(menu.cookProgress() * 24);
        graphics.blitSprite(BURN, 24, 16, 0, 0, x + 79, y + 34, w, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
