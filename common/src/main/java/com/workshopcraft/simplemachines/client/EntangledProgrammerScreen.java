package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.entangled.EntangledLink;
import com.workshopcraft.simplemachines.entangled.EntangledProgrammerBlockEntity;
import com.workshopcraft.simplemachines.entangled.EntangledProgrammerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledProgrammerScreen extends AbstractContainerScreen<EntangledProgrammerMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_programmer.png");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private static final String[] STATUS = {"", "wrote", "no_card", "no_power", "not_owner", "no_code"};
    private EditBox field;
    private Button lock;
    private int shown = -2;

    public EntangledProgrammerScreen(EntangledProgrammerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        final String typed = field == null ? null : field.getValue();
        field = new EditBox(font, leftPos + 12, topPos + 22, 86, 10, Component.translatable("gui.simplemachines.entangled_code")) {
            @Override
            public void insertText(String input) {
                super.insertText(input.replaceAll("[^0-9]", ""));
            }
        };
        field.setBordered(false);
        field.setMaxLength(6);
        field.setTextColor(0xFFE8E8E8);
        addRenderableWidget(field);
        if (typed != null) field.setValue(typed);
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemachines.entangled_random"), b -> press(EntangledProgrammerMenu.BUTTON_RANDOM))
                .bounds(leftPos + 104, topPos + 18, 40, 14).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.simplemachines.entangled_write"), b -> write())
                .bounds(leftPos + 8, topPos + 40, 44, 14).build());
        lock = addRenderableWidget(Button.builder(lockLabel(), b -> press(EntangledProgrammerMenu.BUTTON_PRIVATE))
                .bounds(leftPos + 56, topPos + 40, 50, 14).tooltip(Tooltip.create(Component.translatable("gui.simplemachines.entangled_private_hint"))).build());
        setInitialFocus(field);
    }

    private Component lockLabel() {
        return Component.translatable(menu.isPrivate() ? "gui.simplemachines.entangled_private" : "gui.simplemachines.entangled_public");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.code() != shown) {
            shown = menu.code();
            field.setValue(shown < 0 ? "" : EntangledLink.format(shown));
        }
        lock.setMessage(lockLabel());
    }

    private void write() {
        final String s = field.getValue();
        if (!s.isEmpty()) {
            final int code = Integer.parseInt(s);
            field.setValue(EntangledLink.format(code));
            if (code != menu.code()) press(EntangledProgrammerMenu.BUTTON_SET + code);
        }
        press(EntangledProgrammerMenu.BUTTON_WRITE);
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            minecraft.player.closeContainer();
            return true;
        }
        if (field.isFocused() && event.isConfirmation()) {
            write();
            return true;
        }
        if (field.keyPressed(event) || field.canConsumeInput()) return true;
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        EnergyBar.tag(graphics, font, x, y, BAR_X, BAR_Y, BAR_W, menu.voltage());
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), menu.sendsOnly(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        final int s = Mth.clamp(menu.status(), 0, STATUS.length - 1);
        if (s != EntangledProgrammerBlockEntity.NONE) {
            final Component line = s == EntangledProgrammerBlockEntity.WROTE
                    ? Component.translatable("gui.simplemachines.entangled_wrote", EntangledLink.format(Math.max(0, menu.code())))
                    : Component.translatable("gui.simplemachines.entangled_" + STATUS[s]);
            graphics.text(font, line, 8, 59, s == EntangledProgrammerBlockEntity.WROTE ? 0xFF2E7A2E : 0xFFB02E26, false);
        }
        graphics.text(font, Component.translatable("gui.simplemachines.entangled_cost", EntangledProgrammerBlockEntity.PER_WRITE), 8, 70, 0xFF2D5AA0, false);
    }
}
