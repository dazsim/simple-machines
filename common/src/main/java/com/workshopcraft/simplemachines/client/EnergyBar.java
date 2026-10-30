package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** The tier tag over a machine's energy bar and the bar's hover lines. */
final class EnergyBar {
    private EnergyBar() {
    }

    static void tag(GuiGraphicsExtractor graphics, Font font, int x, int y, int barX, int barY, int barW, int tier) {
        if (tier < 0) return;
        final String s = Voltage.NAME[tier];
        final int ty = y + barY - 10;
        graphics.fill(x + barX, ty - 2, x + barX + barW, ty + 8, 0xFF373737);
        graphics.text(font, s, x + barX + (barW - font.width(s) + 1) / 2, ty, 0xFF000000 | Voltage.COLOR[tier], false);
    }

    static void hover(GuiGraphicsExtractor graphics, Font font, int energy, int capacity, int tier, boolean sendsOnly, int mouseX, int mouseY) {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(PowerTooltip.num(energy) + " / " + PowerTooltip.num(capacity) + " FE"));
        if (tier >= 0) lines.add(Component.translatable(sendsOnly ? "gui.simplemachines.tier_sends" : "gui.simplemachines.tier_accepts",
                PowerTooltip.tier(tier), PowerTooltip.num(Voltage.RATE[tier])).withStyle(ChatFormatting.GRAY));
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
