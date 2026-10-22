package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EnergyScreen extends AbstractContainerScreen<EnergyMenu> {
    private static final Identifier LIT = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private final Identifier texture;

    public EnergyScreen(EnergyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        texture = SimpleMachines.id(menu.generator ? "textures/gui/solid_fuel_generator.png" : "textures/gui/rolling_mill.png");
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
        if (menu.generator) {
            if (menu.progress() > 0) {
                final int h = Mth.ceil(menu.progress() * 13) + 1;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT, 14, 14, 0, 14 - h, x + 80, y + 36 + 14 - h, 14, h);
            }
        } else {
            final int w = Mth.ceil(menu.progress() * 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN, 24, 16, 0, 0, x + 79, y + 34, w, 16);
        }
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            graphics.setTooltipForNextFrame(Component.literal(menu.energy() + " / " + menu.capacity + " FE"), mouseX, mouseY);
        }
    }
}
