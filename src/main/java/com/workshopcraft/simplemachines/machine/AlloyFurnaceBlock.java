package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AlloyFurnaceBlock extends MachineBlock {
    public static final MapCodec<AlloyFurnaceBlock> CODEC = simpleCodec(AlloyFurnaceBlock::new);

    public AlloyFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<AlloyFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<AlloyFurnaceBlockEntity> entityType() {
        return SimpleMachines.alloyFurnaceEntity;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlloyFurnaceBlockEntity(pos, state);
    }
}
