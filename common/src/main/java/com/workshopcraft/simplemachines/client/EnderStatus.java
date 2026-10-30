package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.ender.EnderDeviceMenu;
import com.workshopcraft.simplemachines.ender.EnderLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** The code and owner lines every ender device screen shows. */
final class EnderStatus {
    static final int TITLE = 0xFF404040, TEAL = 0xFF2A8C7C, BLUE = 0xFF2D5AA0, RED = 0xFFB02E26;

    private EnderStatus() {
    }

    static void code(GuiGraphicsExtractor graphics, Font font, EnderDeviceMenu menu, int right, int y) {
        if (menu.code() < 0) return;
        final Component c = label(menu);
        graphics.text(font, c, right - font.width(c), y, TEAL, false);
    }

    static void codeAfter(GuiGraphicsExtractor graphics, Font font, EnderDeviceMenu menu, int x, int y) {
        if (menu.code() >= 0) graphics.text(font, label(menu), x, y, TEAL, false);
    }

    private static Component label(EnderDeviceMenu menu) {
        return Component.translatable("gui.simplemachines.ender_code_label", EnderLink.format(menu.code()));
    }

    static void line(GuiGraphicsExtractor graphics, Font font, EnderDeviceMenu menu, int x, int y) {
        if (menu.code() < 0) graphics.text(font, Component.translatable("gui.simplemachines.ender_no_link"), x, y, RED, false);
        else if (!menu.online()) graphics.text(font, Component.translatable("gui.simplemachines.ender_offline"), x, y, RED, false);
        else graphics.text(font, Component.translatable(menu.isPrivate() ? "gui.simplemachines.ender_owner_private" : "gui.simplemachines.ender_owner_public",
                name(menu.owner())), x, y, BLUE, false);
    }

    static Component name(UUID id) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(id)) return Component.literal(mc.player.getGameProfile().name());
        final PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        return info != null ? Component.literal(info.getProfile().name()) : Component.translatable("gui.simplemachines.ender_someone");
    }
}
