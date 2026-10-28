package com.workshopcraft.simplemachines.quarry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/** A piece of quarry frame: an upright post, a top rail, or a corner where the two meet. */
public class QuarryFrameBlock extends Block {
    public static final MapCodec<QuarryFrameBlock> CODEC = simpleCodec(QuarryFrameBlock::new);
    public static final BooleanProperty POST = BooleanProperty.create("post");
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final VoxelShape POST_SHAPE = box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape HUB = box(5, 10, 5, 11, 16, 11);
    private final Map<BlockState, VoxelShape> shapes = new HashMap<>();

    public QuarryFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POST, true).setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<QuarryFrameBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POST, NORTH, EAST, SOUTH, WEST);
    }

    public static BooleanProperty arm(Direction d) {
        return switch (d) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> WEST;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.computeIfAbsent(state, s -> {
            VoxelShape v = s.getValue(POST) ? POST_SHAPE : HUB;
            if (s.getValue(NORTH)) v = Shapes.or(v, box(5, 10, 0, 11, 16, 5));
            if (s.getValue(SOUTH)) v = Shapes.or(v, box(5, 10, 11, 11, 16, 16));
            if (s.getValue(WEST)) v = Shapes.or(v, box(0, 10, 5, 5, 16, 11));
            if (s.getValue(EAST)) v = Shapes.or(v, box(11, 10, 5, 16, 16, 11));
            return v;
        });
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
