package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** A finished machine as an item; lists the upgrades fitted inside it. */
public class MachineItem extends BlockItem {
    public MachineItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        final List<Upgrade> ups = Upgrade.parse(stack.getOrDefault(SimpleMachines.UPGRADES_FITTED, ""));
        if (!ups.isEmpty()) out.accept(Component.translatable("tooltip.simplemachines.upgrades", Upgrade.summary(Upgrade.pack(ups), false)).withStyle(ChatFormatting.GRAY));
    }
}
