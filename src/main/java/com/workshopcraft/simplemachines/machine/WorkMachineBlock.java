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

/** A chassis-built machine whose behaviour lives entirely in its block entity. */
public class WorkMachineBlock extends PoweredMachineBlock {
    private final Core core;
    private final MapCodec<WorkMachineBlock> codec;

    public WorkMachineBlock(Properties properties, Core core) {
        super(properties);
        this.core = core;
        this.codec = simpleCodec(p -> new WorkMachineBlock(p, core));
    }

    @Override
    protected MapCodec<WorkMachineBlock> codec() {
        return codec;
    }

    @Override
    public Core core() {
        return core;
    }

    @Override
    protected BlockEntityType<?> entityType() {
        return SimpleMachines.MACHINE_ENTITIES.get(core);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entityType().create(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }
}
