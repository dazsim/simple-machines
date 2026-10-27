package com.workshopcraft.simplemachines.gen;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.CableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Two block helical turbine. The base holds the power; the top half is the rotor and needs room above it. */
public class WindTurbineBlock extends Block implements EntityBlock {
    public static final MapCodec<WindTurbineBlock> CODEC = simpleCodec(WindTurbineBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER = Shapes.or(box(1, 0, 1, 15, 2, 15), box(6, 2, 6, 10, 9, 10), box(1, 9, 1, 15, 16, 15));
    private static final VoxelShape UPPER = box(1, 0, 1, 15, 15.5, 15);

    public WindTurbineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<WindTurbineBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        final BlockPos pos = context.getClickedPos();
        final Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) return null;
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        final DoubleBlockHalf half = state.getValue(HALF);
        if (dir == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN)
                && !(neighbour.is(this) && neighbour.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbour, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.preventsBlockDrops() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            final BlockPos below = pos.below();
            final BlockState base = level.getBlockState(below);
            if (base.is(this) && base.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                level.levelEvent(player, 2001, below, Block.getId(base));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new WindTurbineBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != SimpleMachines.windEntity) return null;
        return (l, pos, s, be) -> ((WindTurbineBlockEntity) be).serverTick(server, pos, s);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final BlockPos base = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (level.getBlockEntity(base) instanceof WindTurbineBlockEntity be) {
            be.openness = WindTurbineBlockEntity.measure(level, base);
            player.sendOverlayMessage(Component.translatable("message.simplemachines.wind", String.format("%.1f", be.making(level, base)),
                    Math.round(WindTurbineBlockEntity.height(base) * 100), Math.round(be.openness * 100), be.energy.energy, be.energy.capacity));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CableNetwork.invalidate(level);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) CableNetwork.invalidate(server);
    }
}
