package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.entangled.EntangledRelayMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledRelayScreen extends AbstractContainerScreen<EntangledRelayMenu> {
    private static final ResourceLocation TEXTURE = SimpleMachines.id("textures/gui/entangled_relay.png");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    static final int HEIGHT = 106;
    private Button mode, loader;

    public EntangledRelayScreen(EntangledRelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = HEIGHT;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeLabel(), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EntangledRelayMenu.BUTTON_MODE))
                .bounds(leftPos + 8, topPos + 30, 60, 16).build());
        loader = addRenderableWidget(EntangledStatus.switchButton(this, menu, leftPos + 138, topPos + 87));
        EntangledStatus.refresh(loader, menu);
    }

    private Component modeLabel() {
        return Component.translatable(menu.receiving() ? "gui.simplemachines.relay_receive" : "gui.simplemachines.relay_send");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(modeLabel());
        EntangledStatus.refresh(loader, menu);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        EnergyBar.tag(graphics, font, x, y, BAR_X, BAR_Y, BAR_W, menu.voltage());
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), false, mouseX, mouseY);
        }
        if (EntangledStatus.fitted(menu) && mouseX >= x + 8 && mouseX < x + 134 && mouseY >= y + 89 && mouseY < y + 99) {
            Tips.show(EntangledStatus.chunks(menu));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int xm, int ym) {
        graphics.drawString(font, title, 8, 6, EntangledStatus.TITLE, false);
        EntangledStatus.codeAfter(graphics, font, menu, 8, 18);
        final int n = menu.peers();
        final String peer = menu.receiving() ? (n == 1 ? "sender" : "senders") : (n == 1 ? "receiver" : "receivers");
        graphics.drawString(font, Component.translatable("gui.simplemachines.relay_" + peer, n), 74, 34, EntangledStatus.TITLE, false);
        graphics.drawString(font, Component.translatable(menu.receiving() ? "gui.simplemachines.relay_in_after" : "gui.simplemachines.relay_in",
                PowerTooltip.num(menu.flowIn())), 8, 52, EntangledStatus.TITLE, false);
        graphics.drawString(font, Component.translatable("gui.simplemachines.relay_out", PowerTooltip.num(menu.flowOut())), 8, 62, EntangledStatus.TITLE, false);
        EntangledStatus.line(graphics, font, menu, 8, 78);
        EntangledStatus.loader(graphics, font, menu, 8, 91);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
