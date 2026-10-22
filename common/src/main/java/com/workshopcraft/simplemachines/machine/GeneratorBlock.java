package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratorBlock extends MachineBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);

    public GeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<GeneratorBlockEntity> entityType() {
        return SimpleMachines.generatorEntity;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }
}
