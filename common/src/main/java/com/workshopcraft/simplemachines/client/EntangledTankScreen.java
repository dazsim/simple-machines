package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.ender.EntangledTankMenu;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import com.workshopcraft.simplemachines.pipe.TankItem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class EntangledTankScreen extends AbstractContainerScreen<EntangledTankMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_tank.png");
    private static final int GAUGE_X = 80, GAUGE_Y = 17, GAUGE_W = 16, GAUGE_H = 52;
    private static final int MAX_MB = (int) (TankBlockEntity.CAPACITY / TankBlockEntity.MB);
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;

    public EntangledTankScreen(EntangledTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        final Fluid f = menu.fluid();
        final int fh = f == Fluids.EMPTY ? 0 : Mth.ceil((float) menu.millibuckets() * GAUGE_H / MAX_MB);
        if (fh > 0) {
            final TextureAtlasSprite sprite = ConduitRenderer.fluidSprite(f);
            final int tint = 0xFF000000 | ConduitRenderer.fluidTint(f, minecraft.level, minecraft.player.blockPosition());
            final int bottom = y + GAUGE_Y + GAUGE_H;
            graphics.enableScissor(x + GAUGE_X, bottom - fh, x + GAUGE_X + GAUGE_W, bottom);
            for (int t = bottom - 16; t > bottom - fh - 16; t -= 16) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + GAUGE_X, t, 16, 16, tint);
            graphics.disableScissor();
        }
        for (int i = 0; i < 6; i++) graphics.fill(x + GAUGE_X + GAUGE_W - 6, y + GAUGE_Y + 6 + i * 8, x + GAUGE_X + GAUGE_W, y + GAUGE_Y + 7 + i * 8, 0xFF1F3B5E);
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), menu.sendsOnly(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, 8, 6, EnderStatus.TITLE, false);
        EnderStatus.codeAfter(graphics, font, menu, 8 + font.width(title) + 4, 6);
        final Fluid f = menu.fluid();
        final boolean empty = f == Fluids.EMPTY || menu.millibuckets() <= 0;
        graphics.text(font, empty ? Component.translatable("gui.simplemachines.entangled_tank_empty") : TankItem.fluidName(f), 102, 21,
                empty ? EnderStatus.TITLE : EnderStatus.BLUE, false);
        graphics.text(font, Component.translatable("gui.simplemachines.entangled_tank_amount", buckets(menu.millibuckets()), MAX_MB / 1000), 102, 31, EnderStatus.TITLE, false);
        EnderStatus.line(graphics, font, menu, 8, 72);
    }

    private static String buckets(int mb) {
        return mb % 1000 == 0 ? Integer.toString(mb / 1000) : String.format(java.util.Locale.ROOT, "%.1f", mb / 1000.0);
    }
}
