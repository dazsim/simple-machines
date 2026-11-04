package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Block item with its power lines, if it has any, and a grey hint line or two. */
public class HintBlockItem extends BlockItem {
    private final String[] hints;

    public HintBlockItem(Block block, Properties properties, String... hints) {
        super(block, properties);
        this.hints = hints;
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
        PowerTooltip.append(stack, getBlock(), out);
        for (String h : hints) out.accept(Component.translatable(h).withStyle(ChatFormatting.GRAY));
    }
}
