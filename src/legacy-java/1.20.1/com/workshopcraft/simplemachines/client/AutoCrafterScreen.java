package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.machine.AutoCrafterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {
    private static final ResourceLocation TEXTURE = SimpleMachines.id("textures/gui/auto_crafter.png");
    private static final ResourceLocation BURN = new ResourceLocation("container/furnace/burn_progress");
    private static final ResourceLocation BUTTON = new ResourceLocation("widget/button");
    private static final ResourceLocation BUTTON_HOVER = new ResourceLocation("widget/button_highlighted");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private static final int CLEAR_X = 62, CLEAR_Y = 16, CLEAR_S = 9;
    private static final int RS_X = 67, RS_Y = 55, RS_S = 18;
    private static final int ARROW_X = 89, ARROW_Y = 35;
    private static final String[] MODES = {"always", "with_signal", "without_signal"};
    private static final String[] STATUS = {"idle", "no_recipe", "working", "missing", "full", "no_power", "off"};
    private static final ItemStack[] ICONS = {new ItemStack(Items.GUNPOWDER), new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE_TORCH)};

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 198;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    public int left() {
        return leftPos;
    }

    public int top() {
        return topPos;
    }

    private boolean over(double mx, double my, int x, int y, int s) {
        return mx >= leftPos + x && mx < leftPos + x + s && my >= topPos + y && my < topPos + y + s;
    }

    private void button(GuiGraphics graphics, int x, int y, int s, boolean hover) {
        com.workshopcraft.simplemachines.compat.mc.Gui.blitSprite(graphics, hover ? BUTTON_HOVER : BUTTON, leftPos + x, topPos + y, s, s);
    }

    @Override
    public void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int i = menu.groups(); i < 9; i++) graphics.fill(x + 8 + 18 * i, y + 84, x + 24 + 18 * i, y + 100, 0xFF6E6E6E);
        final int w = Mth.ceil(menu.progress() * 24);
        if (w > 0) com.workshopcraft.simplemachines.compat.mc.Gui.blitSprite(graphics, BURN, 24, 16, 0, 0, x + ARROW_X, y + ARROW_Y, w, 16);
        final boolean overClear = over(mouseX, mouseY, CLEAR_X, CLEAR_Y, CLEAR_S);
        button(graphics, CLEAR_X, CLEAR_Y, CLEAR_S, overClear);
        graphics.drawString(font, "x", x + CLEAR_X + 2, y + CLEAR_Y, 0xFFFFFFFF, true);
        final boolean overRs = over(mouseX, mouseY, RS_X, RS_Y, RS_S);
        button(graphics, RS_X, RS_Y, RS_S, overRs);
        graphics.renderItem(ICONS[Mth.clamp(menu.redstone(), 0, 2)], x + RS_X + 1, y + RS_Y + 1);
        EnergyBar.tag(graphics, font, x, y, BAR_X, BAR_Y, BAR_W, menu.voltage());
        final int h = Mth.ceil(menu.energyFraction() * BAR_H);
        if (h > 0) graphics.fillGradient(x + BAR_X, y + BAR_Y + BAR_H - h, x + BAR_X + BAR_W, y + BAR_Y + BAR_H, 0xFFF4785C, 0xFF9E2A22);
        if (menu.upgrades() != 0) {
            final Component ups = Upgrade.summary(menu.upgrades(), true);
            graphics.drawString(font, ups, x + imageWidth - 8 - font.width(ups), y + inventoryLabelY, 0xFF2D5AA0, false);
        }
        if (mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_W && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_H) {
            EnergyBar.hover(graphics, font, menu.energy(), menu.capacity(), menu.voltage(), menu.sendsOnly(), mouseX, mouseY);
        } else if (overClear) {
            Tips.show(Component.translatable("gui.simplemachines.crafter_clear"));
        } else if (overRs) {
            final List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.simplemachines.redstone"));
            for (int i = 0; i < 3; i++) {
                final Component mode = Component.translatable("gui.simplemachines.redstone_" + MODES[i]);
                lines.add(i == menu.redstone() ? Component.literal("> ").append(mode).withStyle(ChatFormatting.YELLOW)
                        : Component.literal("  ").append(mode).withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.translatable("gui.simplemachines.redstone_click").withStyle(ChatFormatting.BLUE));
            Tips.show(lines);
        } else if (mouseX >= x + ARROW_X && mouseX < x + ARROW_X + 24 && mouseY >= y + ARROW_Y && mouseY < y + ARROW_Y + 16) {
            final int s = Mth.clamp(menu.status(), 0, STATUS.length - 1);
            Tips.show(Component.translatable("gui.simplemachines.crafter_" + STATUS[s]));
        }
    }

    @Override
    public void renderLabels(GuiGraphics graphics, int xm, int ym) {
        super.renderLabels(graphics, xm, ym);
        graphics.drawString(font, Component.translatable("gui.simplemachines.ingredients"), 8, 74, 0xFF404040, false);
        // renderSlot is private here; labels draw right after the slots in the same translated space
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (Slot slot : menu.slots) {
            if (slot instanceof AutoCrafterMenu.Ghost && slot.hasItem()) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, slot.index == AutoCrafterMenu.RESULT ? 0x608B8B8B : 0x80505050);
            }
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        if (b == 0 && over(mx, my, CLEAR_X, CLEAR_Y, CLEAR_S)) {
            press(AutoCrafterMenu.BUTTON_CLEAR);
            return true;
        }
        if ((b == 0 || b == 1) && over(mx, my, RS_X, RS_Y, RS_S)) {
            press(b == 0 ? AutoCrafterMenu.BUTTON_REDSTONE : AutoCrafterMenu.BUTTON_REDSTONE_BACK);
            return true;
        }
        return super.mouseClicked(mx, my, b);
    }

    public void press(int id) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        for (int b : AutoCrafterMenu.bytePresses(id)) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
