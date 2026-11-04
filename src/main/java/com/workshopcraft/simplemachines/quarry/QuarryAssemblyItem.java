package com.workshopcraft.simplemachines.quarry;

import com.workshopcraft.simplemachines.cable.Voltage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Quarry assembly; shows the drill built into it. */
public class QuarryAssemblyItem extends Item {
    public QuarryAssemblyItem(Properties properties) {
        super(properties);
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        final Consumer<Component> out = lines::add;
        super.appendHoverText(stack, context, lines, flag);
    *///?}
        out.accept(Component.translatable("tooltip.simplemachines.builds_quarry", com.workshopcraft.simplemachines.item.PowerTooltip.tier(Voltage.MV))
                .withStyle(ChatFormatting.GRAY));
        QuarryDrill.tooltip(stack, out);
    }
}
