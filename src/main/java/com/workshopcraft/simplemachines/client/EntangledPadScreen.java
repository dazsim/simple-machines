package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.entangled.EntangledPadBlockEntity;
import com.workshopcraft.simplemachines.entangled.EntangledPadMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class EntangledPadScreen extends AbstractContainerScreen<EntangledPadMenu> {
    private static final Identifier TEXTURE = SimpleMachines.id("textures/gui/entangled_pad.png");
    private static final int BAR_X = 153, BAR_Y = 17, BAR_W = 14, BAR_H = 52;
    private static final int GREEN = 0xFF2E8B2E;
    private static final String[] DIMS = {"", "overworld", "nether", "end", "other"};

    public EntangledPadScreen(EntangledPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 104);
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

    private void text(GuiGraphicsExtractor graphics, Component c, int y, int color) {
        graphics.text(font, c, 8, y, color, false);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, 8, 6, EntangledStatus.TITLE, false);
        EntangledStatus.code(graphics, font, menu, 146, 6);
        final int status = menu.status();
        if (status != EntangledPadBlockEntity.NO_CODE) {
            if (menu.dimension() > 0) {
                text(graphics, Component.translatable("gui.simplemachines.pad_linked",
                        Component.translatable("gui.simplemachines.pad_dim_" + DIMS[Math.min(menu.dimension(), 4)])), 20, EntangledStatus.TITLE);
                final BlockPos t = menu.target();
                text(graphics, Component.literal(t.getX() + ", " + t.getY() + ", " + t.getZ()), 30, EntangledStatus.TITLE);
                text(graphics, Component.translatable("gui.simplemachines.pad_trip", PowerTooltip.num(menu.cost())), 42, EntangledStatus.TITLE);
            }
            final Component s = switch (status) {
                case EntangledPadBlockEntity.READY -> Component.translatable("gui.simplemachines.pad_ready");
                case EntangledPadBlockEntity.ALONE -> Component.translatable("gui.simplemachines.pad_alone");
                case EntangledPadBlockEntity.FULL -> Component.translatable("gui.simplemachines.pad_full");
                default -> Component.translatable("gui.simplemachines.pad_low", PowerTooltip.num(menu.cost()));
            };
            text(graphics, s, 54, status == EntangledPadBlockEntity.READY ? GREEN : EntangledStatus.RED);
        }
        if (menu.code() < 0) {
            graphics.textWithWordWrap(font, Component.translatable("gui.simplemachines.entangled_no_link"), 8, 20, 136, EntangledStatus.RED, false);
        } else {
            EntangledStatus.line(graphics, font, menu, 8, 80);
        }
        text(graphics, Component.translatable("gui.simplemachines.entangled_fe", PowerTooltip.num(menu.energy()), PowerTooltip.num(menu.capacity())),
                92, EntangledStatus.TITLE);
    }
}
