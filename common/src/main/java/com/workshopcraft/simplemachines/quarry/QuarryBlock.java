package com.workshopcraft.simplemachines.quarry;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.machine.WorkMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The quarry: up to three pipe filters fit into it to choose what it keeps; sneak with an empty hand takes the last one back. */
public class QuarryBlock extends WorkMachineBlock {
    public QuarryBlock(Properties properties) {
        super(properties, Core.QUARRY);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!held.is(SimpleMachines.pipeFilter)) return super.useItemOn(held, state, level, pos, player, hand, hit);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof QuarryBlockEntity be) {
            if (!be.addFilter(held)) {
                player.sendOverlayMessage(Component.translatable("message.simplemachines.quarry_filter_full", QuarryBlockEntity.FILTERS));
            } else {
                held.consume(1, player);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                player.sendOverlayMessage(Component.translatable("message.simplemachines.quarry_filter_on", be.filters().size(), QuarryBlockEntity.FILTERS));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive() && level.getBlockEntity(pos) instanceof QuarryBlockEntity be && !be.filters().isEmpty()) {
            if (!level.isClientSide()) {
                Block.popResource(level, pos.above(), be.takeFilter());
                player.sendOverlayMessage(Component.translatable("message.simplemachines.quarry_filter_off"));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
