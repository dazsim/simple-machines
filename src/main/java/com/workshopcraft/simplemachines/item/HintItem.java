package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A plain item with grey hint lines. */
public class HintItem extends Item {
    private final String[] hints;

    public HintItem(Properties properties, String... hints) {
        super(properties);
        this.hints = hints;
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        final Consumer<Component> out = lines::add;
    *///?}
        for (String h : hints) out.accept(Component.translatable(h).withStyle(ChatFormatting.GRAY));
    }
}
