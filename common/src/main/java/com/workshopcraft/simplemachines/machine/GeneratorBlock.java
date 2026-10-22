package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GeneratorBlock extends PoweredMachineBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);

    public GeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GeneratorBlock> codec() {
        return CODEC;
    }

    @Override
    public Core core() {
        return Core.GENERATOR;
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
