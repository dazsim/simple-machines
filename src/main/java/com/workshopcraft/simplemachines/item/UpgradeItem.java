package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.ServerConfig;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.entangled.EntangledDeviceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class UpgradeItem extends Item {
    private final Upgrade upgrade;

    public UpgradeItem(Upgrade upgrade, Properties properties) {
        super(properties);
        this.upgrade = upgrade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.simplemachines." + upgrade.id).withStyle(ChatFormatting.BLUE));
        if (!upgrade.module) {
            out.accept(Component.translatable("tooltip.simplemachines.upgrade_fit").withStyle(ChatFormatting.GRAY));
            return;
        }
        final int fe = ServerConfig.DEFAULT_FE_PER_CHUNK;
        if (upgrade == Upgrade.CHUNK_LOADER) out.accept(Component.translatable("tooltip.simplemachines.chunk_loader_cost", fe,
                EntangledDeviceBlockEntity.UPKEEP).withStyle(ChatFormatting.GRAY));
        else out.accept(Component.translatable("tooltip.simplemachines.range_module_cost", 9 * fe, 25 * fe).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.module_cap", PowerTooltip.chunks(ServerConfig.DEFAULT_MAX_CHUNKS)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines." + upgrade.id + "_fit").withStyle(ChatFormatting.DARK_GRAY));
    }
}
