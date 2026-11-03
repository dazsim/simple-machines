package com.workshopcraft.simplemachines.pipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/** A full block tank, so a pipe can reach it from any side. */
public class TankBlock extends Block implements EntityBlock {
    public static final MapCodec<TankBlock> CODEC = simpleCodec(TankBlock::new);

    public TankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TankBlockEntity tank)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (held.is(Items.BUCKET)) {
            final Fluid f = tank.fluid();
            if (tank.amount() < TankBlockEntity.BUCKET || f.getBucket() == Items.AIR) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                tank.extract(f, TankBlockEntity.BUCKET, false);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(f.getBucket())));
                f.getPickupSound().ifPresent(s -> level.playSound(null, pos, s, SoundSource.BLOCKS, 1F, 1F));
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (held.getItem() instanceof BucketItem bucket) {
            final Fluid f = bucket.getContent();
            if (f == Fluids.EMPTY) return InteractionResult.TRY_WITH_EMPTY_HAND;
            // full or a different fluid: do not spill it beside the tank
            if (tank.room(f) < TankBlockEntity.BUCKET) return InteractionResult.CONSUME;
            if (!level.isClientSide()) {
                tank.insert(f, TankBlockEntity.BUCKET, false);
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                final SoundEvent s = f.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
                level.playSound(null, pos, s, SoundSource.BLOCKS, 1F, 1F);
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
