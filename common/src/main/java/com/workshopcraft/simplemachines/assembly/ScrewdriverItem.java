package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.machine.PoweredMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Use on a chassis to screw the loose part in; sneak-use to take the last part off or open a machine back up. */
public class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);
        final Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        final boolean sneak = player.isSecondaryUseActive();
        if (state.getBlock() instanceof ChassisBlock) {
            if (!level.isClientSide()) {
                if (sneak) Assembly.unscrew(state, level, pos);
                else Assembly.screw(state, level, pos, player);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getBlock() instanceof PoweredMachineBlock && sneak) {
            if (!level.isClientSide()) Assembly.disassemble(state, level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
