package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
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
    private static final int TANK_X = 72, TANK_Y = 17, TANK_W = 32, TANK_H = 52;
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
        if (menu.upgrades() != 0) {
            final Component ups = Upgrade.summary(menu.upgrades(), true);
            graphics.text(font, ups, x + imageWidth - 8 - font.width(ups), y + inventoryLabelY, 0xFF2D5AA0, false);
        }
        if (layout == Layout.LAVA) {
            final int mb = menu.value(0);
            final int cap = Math.max(1, menu.value(1));
            final int lh = Mth.ceil(Mth.clamp((float) mb / cap, 0, 1) * TANK_H);
            if (lh > 0) graphics.fillGradient(x + TANK_X, y + TANK_Y + TANK_H - lh, x + TANK_X + TANK_W, y + TANK_Y + TANK_H, 0xFFFFB13B, 0xFFC2410E);
            for (int i = 1; i < 4; i++) graphics.fill(x + TANK_X, y + TANK_Y + i * TANK_H / 4, x + TANK_X + 6, y + TANK_Y + i * TANK_H / 4 + 1, 0xFF3A3A3A);
            if (mouseX >= x + TANK_X && mouseX < x + TANK_X + TANK_W && mouseY >= y + TANK_Y && mouseY < y + TANK_Y + TANK_H) {
                graphics.setTooltipForNextFrame(Component.translatable("gui.simplemachines.lava_tank", mb, cap), mouseX, mouseY);
            }
        }
        if (layout == Layout.GRID && (menu.value(5) & 1) != 0) {
            final Component blocked = Component.translatable("gui.simplemachines.quarry_blocked");
            graphics.text(font, blocked, x + 8, y + inventoryLabelY - 10, 0xFFB02020, false);
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
