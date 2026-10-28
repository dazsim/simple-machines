package com.workshopcraft.simplemachines.gen;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One helix segment of a wind turbine. STACK is its place above a base, 1 to 5, and 0 when it has no base
 * or is a sixth blade or higher; those do nothing and are drawn still. Blades in a stack are drawn by the base.
 */
public class TurbineBladeBlock extends Block {
    public static final MapCodec<TurbineBladeBlock> CODEC = simpleCodec(TurbineBladeBlock::new);
    public static final int MAX = 5;
    public static final IntegerProperty STACK = IntegerProperty.create("stack", 0, MAX);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    public TurbineBladeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STACK, 0));
    }

    @Override
    protected MapCodec<TurbineBladeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STACK);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(STACK) > 0 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Place in the stack for a blade sitting on the given block. */
    public static int place(BlockState below) {
        if (below.is(SimpleMachines.windTurbine) && below.getValue(WindTurbineBlock.HALF) == DoubleBlockHalf.LOWER) return 1;
        if (below.is(SimpleMachines.turbineBlade)) {
            final int n = below.getValue(STACK);
            return n > 0 && n < MAX ? n + 1 : 0;
        }
        return 0;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(STACK, place(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (dir == Direction.DOWN) return state.setValue(STACK, place(neighbour));
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    /** Commands and structures skip getStateForPlacement, so settle the stack here too. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        final int n = place(level.getBlockState(pos.below()));
        if (state.getValue(STACK) != n) level.setBlock(pos, state.setValue(STACK, n), Block.UPDATE_ALL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final int n = state.getValue(STACK);
        final BlockPos base = pos.below(Math.max(n, 1));
        if (n > 0 && level.getBlockEntity(base) instanceof WindTurbineBlockEntity be) player.sendOverlayMessage(be.status(level, base));
        else player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.simplemachines.blade_idle", MAX));
        return InteractionResult.SUCCESS;
    }
}
