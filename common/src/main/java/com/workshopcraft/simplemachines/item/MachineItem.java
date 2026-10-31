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

/** A finished machine as an item; lists its power, then the upgrades fitted inside it. */
public class MachineItem extends BlockItem {
    public MachineItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        final List<Upgrade> ups = Upgrade.parse(stack.getOrDefault(SimpleMachines.UPGRADES_FITTED, ""));
        final com.workshopcraft.simplemachines.ender.EnderLink link = stack.get(SimpleMachines.ENDER_LINK);
        if (link != null) link.tooltip(out);
        if (link != null && link.priv()) out.accept(Component.translatable("tooltip.simplemachines.ender_private_pipes").withStyle(ChatFormatting.GRAY));
        PowerTooltip.append(stack, getBlock(), out);
        com.workshopcraft.simplemachines.quarry.QuarryDrill.tooltip(stack, out);
        final com.workshopcraft.simplemachines.pipe.TankContents lava = stack.get(SimpleMachines.TANK);
        if (lava != null && lava.amount() > 0) out.accept(Component.translatable("tooltip.simplemachines.tank",
                com.workshopcraft.simplemachines.pipe.TankItem.fluidName(lava.fluid()), lava.amount() / com.workshopcraft.simplemachines.pipe.TankBlockEntity.MB,
                com.workshopcraft.simplemachines.machine.LavaGeneratorBlockEntity.TANK_MB).withStyle(ChatFormatting.GRAY));
        if (!ups.isEmpty()) out.accept(Component.translatable("tooltip.simplemachines.upgrades", Upgrade.summary(Upgrade.pack(ups), false)).withStyle(ChatFormatting.GRAY));
    }
}
