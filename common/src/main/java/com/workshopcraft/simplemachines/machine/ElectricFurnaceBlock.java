package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlock extends PoweredMachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = simpleCodec(ElectricFurnaceBlock::new);

    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ElectricFurnaceBlock> codec() {
        return CODEC;
    }

    @Override
    public Core core() {
        return Core.FURNACE;
    }

    @Override
    protected BlockEntityType<ElectricFurnaceBlockEntity> entityType() {
        return SimpleMachines.electricFurnaceEntity;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
