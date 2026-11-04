package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.FilterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private static final ResourceLocation TEXTURE = SimpleMachines.id("textures/gui/pipe_filter.png");
    private static final String[] KEYS = {"mode", "tags", "data"};
    private static final String[][] VALUES = {{"allow", "deny"}, {"off", "on"}, {"ignore", "match"}};
    private final Button[] buttons = new Button[3];

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 3; i++) {
            final int id = i;
            buttons[i] = addRenderableWidget(Button.builder(label(i), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                    .bounds(leftPos + 96, topPos + 17 + i * 18, 72, 18)
                    .tooltip(Tooltip.create(Component.translatable("gui.simplemachines.filter_" + KEYS[i] + "_hint"))).build());
        }
    }

    private Component label(int i) {
        final String value = VALUES[i][menu.setting(i) ? 1 : 0];
        return Component.translatable("gui.simplemachines.filter_" + KEYS[i], Component.translatable("gui.simplemachines.filter_" + value));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < 3; i++) buttons[i].setMessage(label(i));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
