package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Diamond drill bit or diamond-tipped chain, screwed onto a drill or chainsaw on a Tool Stand. */
public class ToolTipItem extends Item {
    private final String hint;

    public ToolTipItem(Properties properties, String hint) {
        super(properties);
        this.hint = hint;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.tool_part").withStyle(ChatFormatting.DARK_GRAY));
    }
}
