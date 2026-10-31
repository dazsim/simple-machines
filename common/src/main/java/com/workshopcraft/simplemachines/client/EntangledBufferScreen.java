package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.ender.EntangledBufferMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledBufferScreen extends AbstractContainerScreen<EntangledBufferMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_buffer.png");
    private static final int BAR_X = 8, BAR_Y = 88, BAR_W = 160, BAR_H = 6;

    public EntangledBufferScreen(EntangledBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, EntangledBufferMenu.HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        final int w = Mth.ceil(menu.energyFraction() * BAR_W);
        if (w > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y, x + BAR_X + w, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), menu.sendsOnly(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, 8, 6, EnderStatus.TITLE, false);
        EnderStatus.code(graphics, font, menu, 168, 6);
        EnderStatus.line(graphics, font, menu, 8, 76);
        graphics.text(font, Component.translatable("gui.simplemachines.ender_fe",
                PowerTooltip.num(menu.energy()), PowerTooltip.num(menu.capacity())), 8, 98, EnderStatus.TITLE, false);
    }
}
