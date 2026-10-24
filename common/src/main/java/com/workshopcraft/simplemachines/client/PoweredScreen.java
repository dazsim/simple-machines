package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.Layout;
import com.workshopcraft.simplemachines.machine.PoweredMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class PoweredScreen extends AbstractContainerScreen<PoweredMenu> {
    private static final Identifier BURN = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private final Identifier texture;

    public PoweredScreen(PoweredMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        texture = SimpleMachines.id("textures/gui/" + menu.layout.name() + ".png");
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
        final Layout layout = menu.layout;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        if (layout.arrowX() >= 0) {
            final int w = Mth.ceil(menu.progress() * 24);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN, 24, 16, 0, 0, x + layout.arrowX(), y + layout.arrowY(), w, 16);
        }
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        final Component fe = Component.literal(menu.energy() + " / " + menu.capacity() + " FE");
        if (layout.slots().length == 0) graphics.text(font, fe, x + (BAR_X - font.width(fe)) / 2 + 4, y + 39, 0xFF404040, false);
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            graphics.setTooltipForNextFrame(fe, mouseX, mouseY);
        }
    }
}
