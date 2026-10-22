package com.workshopcraft.simplemachines.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ExtruderBlock extends MachineBlock {
    public static final MapCodec<ExtruderBlock> CODEC = simpleCodec(ExtruderBlock::new);

    public ExtruderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ExtruderBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockEntityType<ExtruderBlockEntity> entityType() {
        return SimpleMachines.extruderEntity;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtruderBlockEntity(pos, state);
    }
}
