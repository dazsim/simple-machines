package com.workshopcraft.simplemachines.client;

import com.workshopcraft.simplemachines.entangled.EntangledDeviceBlockEntity;
import com.workshopcraft.simplemachines.entangled.EntangledDeviceMenu;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import com.workshopcraft.simplemachines.entangled.EntangledLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** The code and owner lines every entangled device screen shows. */
final class EntangledStatus {
    static final int TITLE = 0xFF404040, TEAL = 0xFF2A8C7C, BLUE = 0xFF2D5AA0, RED = 0xFFB02E26, GREEN = 0xFF3C7A2A, GREY = 0xFF6E6E6E;

    private EntangledStatus() {
    }

    static void code(GuiGraphicsExtractor graphics, Font font, EntangledDeviceMenu menu, int right, int y) {
        if (menu.code() < 0) return;
        final Component c = label(menu);
        graphics.text(font, c, right - font.width(c), y, TEAL, false);
    }

    static void codeAfter(GuiGraphicsExtractor graphics, Font font, EntangledDeviceMenu menu, int x, int y) {
        if (menu.code() >= 0) graphics.text(font, label(menu), x, y, TEAL, false);
    }

    private static Component label(EntangledDeviceMenu menu) {
        return Component.translatable("gui.simplemachines.entangled_code_label", EntangledLink.format(menu.code()));
    }

    static void line(GuiGraphicsExtractor graphics, Font font, EntangledDeviceMenu menu, int x, int y) {
        if (menu.code() < 0) graphics.text(font, Component.translatable("gui.simplemachines.entangled_no_link"), x, y, RED, false);
        else if (!menu.online()) graphics.text(font, Component.translatable("gui.simplemachines.entangled_offline"), x, y, RED, false);
        else graphics.text(font, Component.translatable(menu.isPrivate() ? "gui.simplemachines.entangled_owner_private" : "gui.simplemachines.entangled_owner_public",
                name(menu.owner())), x, y, BLUE, false);
    }

    /** The Chunk Loader line: what it covers and costs, or why it is not loading. */
    static void loader(GuiGraphicsExtractor graphics, Font font, EntangledDeviceMenu menu, int x, int y) {
        final int side = 2 * menu.loaderRadius() + 1;
        final EntangledDeviceBlockEntity.LoaderStatus st = menu.loaderStatus();
        final Component text = switch (st) {
            case NONE -> Component.translatable("gui.simplemachines.loader_none");
            case OFF -> Component.translatable("gui.simplemachines.loader_off");
            case DISABLED -> Component.translatable("gui.simplemachines.loader_disabled");
            case NO_CODE -> Component.translatable("gui.simplemachines.loader_no_code");
            case ACTIVE -> Component.translatable("gui.simplemachines.loader_active", side, side, PowerTooltip.num(menu.loaderDraw()));
            case AWAY -> Component.translatable("gui.simplemachines.loader_away");
            case GRACE -> Component.translatable("gui.simplemachines.loader_grace", menu.graceSeconds());
            case NO_POWER -> Component.translatable("gui.simplemachines.loader_no_power",
                    PowerTooltip.num(Math.min(menu.capacity(), EntangledDeviceBlockEntity.RESTART_TICKS * (menu.loaderDraw() + EntangledDeviceBlockEntity.UPKEEP))));
            case OVER_LIMIT -> Component.translatable("gui.simplemachines.loader_over", PowerTooltip.chunks(menu.chunkCap()));
        };
        final int colour = switch (st) {
            case ACTIVE -> GREEN;
            case NONE, OFF, AWAY -> GREY;
            default -> RED;
        };
        graphics.text(font, text, x, y, colour, false);
    }

    static boolean fitted(EntangledDeviceMenu menu) {
        return menu.loaderStatus() != EntangledDeviceBlockEntity.LoaderStatus.NONE;
    }

    /** "9 chunks, you use 18/75", or no cap shown when the server has none. */
    static Component chunks(EntangledDeviceMenu menu) {
        final int side = 2 * menu.loaderRadius() + 1;
        return menu.chunkCap() > 0 ? Component.translatable("gui.simplemachines.loader_chunks", PowerTooltip.chunks(side * side), menu.chunksUsed(), menu.chunkCap())
                : Component.translatable("gui.simplemachines.loader_chunks_uncapped", PowerTooltip.chunks(side * side), menu.chunksUsed());
    }

    static Component switchLabel(EntangledDeviceMenu menu) {
        return Component.translatable(menu.loaderOn() ? "gui.simplemachines.loader_switch_on" : "gui.simplemachines.loader_switch_off");
    }

    static net.minecraft.client.gui.components.Button switchButton(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
                                                                   EntangledDeviceMenu menu, int x, int y) {
        return net.minecraft.client.gui.components.Button.builder(switchLabel(menu), b -> Minecraft.getInstance().gameMode
                        .handleInventoryButtonClick(menu.containerId, EntangledDeviceMenu.BUTTON_LOADER))
                .bounds(x, y, 30, 14).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.simplemachines.loader_switch_hint"))).build();
    }

    static void refresh(net.minecraft.client.gui.components.Button button, EntangledDeviceMenu menu) {
        final EntangledDeviceBlockEntity.LoaderStatus st = menu.loaderStatus();
        button.visible = st != EntangledDeviceBlockEntity.LoaderStatus.NONE && st != EntangledDeviceBlockEntity.LoaderStatus.DISABLED;
        button.setMessage(switchLabel(menu));
    }

    static Component name(UUID id) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(id)) return Component.literal(mc.player.getGameProfile().name());
        final PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        return info != null ? Component.literal(info.getProfile().name()) : Component.translatable("gui.simplemachines.entangled_someone");
    }
}
