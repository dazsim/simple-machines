package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.entangled.EntangledBufferMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledBufferScreen extends AbstractContainerScreen<EntangledBufferMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_buffer.png");
    private static final int BAR_X = 8, BAR_Y = 107, BAR_W = 70, BAR_H = 6;
    static final int MODULE_X = 115, MODULE_Y = 106;
    private Button loader;

    public EntangledBufferScreen(EntangledBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, EntangledBufferMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        loader = addRenderableWidget(EntangledStatus.switchButton(this, menu, leftPos + 81, topPos + 105));
        EntangledStatus.refresh(loader, menu);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        EntangledStatus.refresh(loader, menu);
    }

    /** The fitted modules, loader first, as they sit in the device's slots. */
    private List<Upgrade> modules() {
        final List<Upgrade> out = new ArrayList<>();
        for (Upgrade u : Upgrade.values()) if (u.module) for (int i = 0; i < Upgrade.count(menu.upgrades(), u); i++) out.add(u);
        return out;
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
        final List<Upgrade> modules = modules();
        for (int i = 0; i < modules.size() && i < Upgrade.MAX_SLOTS; i++) {
            final int sx = x + MODULE_X + i * 18, sy = y + MODULE_Y;
            graphics.item(new ItemStack(modules.get(i).item()), sx, sy);
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                graphics.setTooltipForNextFrame(font, Component.translatable("item.simplemachines." + modules.get(i).id), mouseX, mouseY);
            }
        }
        if (modules.isEmpty() && mouseX >= x + MODULE_X && mouseX < x + MODULE_X + 52 && mouseY >= y + MODULE_Y && mouseY < y + MODULE_Y + 16) {
            graphics.setTooltipForNextFrame(font, Component.translatable("gui.simplemachines.module_slots_hint"), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, 8, 6, EntangledStatus.TITLE, false);
        EntangledStatus.code(graphics, font, menu, 168, 6);
        EntangledStatus.line(graphics, font, menu, 8, 75);
        EntangledStatus.loader(graphics, font, menu, 8, 85);
        if (EntangledStatus.fitted(menu)) graphics.text(font, EntangledStatus.chunks(menu), 8, 95, EntangledStatus.GREY, false);
        graphics.text(font, Component.translatable("gui.simplemachines.entangled_fe",
                PowerTooltip.num(menu.energy()), PowerTooltip.num(menu.capacity())), 8, 117, EntangledStatus.TITLE, false);
    }
}
