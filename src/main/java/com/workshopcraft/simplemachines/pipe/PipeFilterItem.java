package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Fits on a pipe end that delivers into an inventory or tank, and keeps its settings when taken off. */
public class PipeFilterItem extends Item {
    public PipeFilterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final PipeFilter f = stack.get(SimpleMachines.FILTER);
        if (f == null || f.equals(PipeFilter.EMPTY)) {
            out.accept(Component.translatable("tooltip.simplemachines.pipe_filter").withStyle(ChatFormatting.GRAY));
            return;
        }
        out.accept(Component.translatable(f.deny ? "gui.simplemachines.filter_deny" : "gui.simplemachines.filter_allow")
                .append(f.tags ? ", " : "").append(f.tags ? Component.translatable("tooltip.simplemachines.filter_tags") : Component.empty())
                .append(f.data ? ", " : "").append(f.data ? Component.translatable("tooltip.simplemachines.filter_data") : Component.empty())
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack s : f.samples()) out.accept(Component.literal(" ").append(s.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
