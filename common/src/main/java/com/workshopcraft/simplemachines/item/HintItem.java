package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (String h : hints) out.accept(Component.translatable(h).withStyle(ChatFormatting.GRAY));
    }
}
