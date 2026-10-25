package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/** Tool battery; keeps whatever charge the tool had when it was taken off. */
public class BatteryModuleItem extends Item {
    public BatteryModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final int e = PoweredToolItem.energy(stack);
        if (e > 0) out.accept(Component.translatable("tooltip.simplemachines.stored", NumberFormat.getIntegerInstance(Locale.ROOT).format(e)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.tool_part").withStyle(ChatFormatting.DARK_GRAY));
    }
}
