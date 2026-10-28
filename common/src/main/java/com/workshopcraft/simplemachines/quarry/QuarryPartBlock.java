package com.workshopcraft.simplemachines.quarry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Landmark post, frame beam or drill head: a plain block with its own outline. */
public class QuarryPartBlock extends Block {
    public static final VoxelShape POST = box(6, 0, 6, 10, 12, 10);
    public static final VoxelShape HEAD = Shapes.or(box(3, 8, 3, 13, 16, 13), box(6, 0, 6, 10, 8, 10));
    private final VoxelShape shape;
    private final MapCodec<QuarryPartBlock> codec;

    public QuarryPartBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        this.codec = simpleCodec(p -> new QuarryPartBlock(p, shape));
    }

    @Override
    protected MapCodec<QuarryPartBlock> codec() {
        return codec;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
