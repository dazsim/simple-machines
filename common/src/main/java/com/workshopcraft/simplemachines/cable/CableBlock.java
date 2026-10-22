package com.workshopcraft.simplemachines.cable;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Redstone Copper power cable. Joins other cables of this mod and anything exposing FE, nothing else. */
public class CableBlock extends Block {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    public static final int RATE = 1000;
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        final VoxelShape core = Block.box(6, 6, 6, 10, 10, 10);
        final VoxelShape[] arms = {
                Block.box(6, 0, 6, 10, 6, 10), Block.box(6, 10, 6, 10, 16, 10),
                Block.box(6, 6, 0, 10, 10, 6), Block.box(6, 6, 10, 10, 10, 16),
                Block.box(0, 6, 6, 6, 10, 10), Block.box(10, 6, 6, 16, 10, 10)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) if ((mask & 1 << i) != 0) shape = Shapes.or(shape, arms[i]);
            SHAPES[mask] = shape;
        }
    }

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values()) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<CableBlock> codec() {
        return CODEC;
    }

    private static boolean connects(LevelReader level, BlockPos pos, Direction dir, BlockState neighbour, boolean current) {
        if (neighbour.getBlock() instanceof CableBlock) return true;
        if (neighbour.isAir()) return false;
        if (!(level instanceof Level real)) return current;
        return EnergyBuffer.lookup.has(real, pos.relative(dir), dir.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(d),
                    connects(level, pos, d, level.getBlockState(pos.relative(d)), false));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        final BooleanProperty p = PipeBlock.PROPERTY_BY_DIRECTION.get(dir);
        final BlockState next = state.setValue(p, connects(level, pos, dir, neighbour, state.getValue(p)));
        if (next != state && level instanceof ServerLevel server) CableNetwork.invalidate(server);
        return next;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) CableNetwork.invalidate(server);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CableNetwork.invalidate(level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        final Direction[] dirs = Direction.values();
        for (int i = 0; i < 6; i++) if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dirs[i]))) mask |= 1 << i;
        return SHAPES[mask];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
    }
}
