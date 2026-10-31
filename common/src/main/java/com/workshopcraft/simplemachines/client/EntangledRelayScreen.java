package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.entangled.EntangledRelayMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledRelayScreen extends AbstractContainerScreen<EntangledRelayMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_relay.png");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private Button mode;

    public EntangledRelayScreen(EntangledRelayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 92);
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeLabel(), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, EntangledRelayMenu.BUTTON_MODE))
                .bounds(leftPos + 8, topPos + 30, 60, 16).build());
    }

    private Component modeLabel() {
        return Component.translatable(menu.receiving() ? "gui.simplemachines.relay_receive" : "gui.simplemachines.relay_send");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(modeLabel());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        EnergyBar.tag(graphics, font, x, y, BAR_X, BAR_Y, BAR_W, menu.voltage());
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), false, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, 8, 6, EntangledStatus.TITLE, false);
        EntangledStatus.codeAfter(graphics, font, menu, 8, 18);
        final int n = menu.peers();
        final String peer = menu.receiving() ? (n == 1 ? "sender" : "senders") : (n == 1 ? "receiver" : "receivers");
        graphics.text(font, Component.translatable("gui.simplemachines.relay_" + peer, n), 74, 34, EntangledStatus.TITLE, false);
        graphics.text(font, Component.translatable(menu.receiving() ? "gui.simplemachines.relay_in_after" : "gui.simplemachines.relay_in",
                PowerTooltip.num(menu.flowIn())), 8, 52, EntangledStatus.TITLE, false);
        graphics.text(font, Component.translatable("gui.simplemachines.relay_out", PowerTooltip.num(menu.flowOut())), 8, 62, EntangledStatus.TITLE, false);
        EntangledStatus.line(graphics, font, menu, 8, 78);
    }
}
