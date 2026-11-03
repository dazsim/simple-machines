package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.assembly.Core;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** The battery, with its charge gauge as a block state: five bars of 20%. */
public class BatteryBlock extends WorkMachineBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);
    private static final MapCodec<WorkMachineBlock> CODEC = simpleCodec(BatteryBlock::new);

    public BatteryBlock(Properties properties) {
        super(properties, Core.BATTERY);
    }

    @Override
    protected MapCodec<WorkMachineBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL);
    }
}
