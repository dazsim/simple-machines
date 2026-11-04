package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Tool battery; keeps whatever charge the tool had when it was taken off. */
public class BatteryModuleItem extends Item {
    public BatteryModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        final Consumer<Component> out = lines::add;
    *///?}
        final int e = PoweredToolItem.energy(stack);
        out.accept((e > PoweredToolItem.CAPACITY ? Component.translatable("tooltip.simplemachines.stored", PowerTooltip.num(e))
                : Component.translatable("tooltip.simplemachines.charge", PowerTooltip.num(e), PowerTooltip.num(PoweredToolItem.CAPACITY))).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.tool_part").withStyle(ChatFormatting.DARK_GRAY));
    }
}
