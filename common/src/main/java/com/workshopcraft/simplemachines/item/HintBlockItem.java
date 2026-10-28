package com.workshopcraft.simplemachines.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Block item with a grey hint line or two under its name. */
public class HintBlockItem extends BlockItem {
    private final String[] hints;

    public HintBlockItem(Block block, Properties properties, String... hints) {
        super(block, properties);
        this.hints = hints;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        for (String h : hints) out.accept(Component.translatable(h).withStyle(ChatFormatting.GRAY));
    }
}
