package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.assembly.Upgrade;
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
        out.accept(Component.translatable("tooltip.simplemachines.upgrade_fit").withStyle(ChatFormatting.GRAY));
    }
}
