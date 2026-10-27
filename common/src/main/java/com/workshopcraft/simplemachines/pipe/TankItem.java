package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Consumer;

public class TankItem extends BlockItem {
    public TankItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static Component fluidName(Fluid f) {
        return f.defaultFluidState().createLegacyBlock().getBlock().getName();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final TankContents c = stack.get(SimpleMachines.TANK);
        if (c == null || c.amount() <= 0) {
            out.accept(Component.translatable("tooltip.simplemachines.tank_empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        out.accept(Component.translatable("tooltip.simplemachines.tank", fluidName(c.fluid()),
                c.amount() / TankBlockEntity.MB, TankBlockEntity.CAPACITY / TankBlockEntity.MB).withStyle(ChatFormatting.GRAY));
    }
}
