package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Finish;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** A machine built on a chassis. Remembers the chassis tier and the panel finish; the worktop lives in the block entity. */
public abstract class PoweredMachineBlock extends MachineBlock {
    public static final EnumProperty<Tier> TIER = EnumProperty.create("tier", Tier.class);
    public static final EnumProperty<Finish> PANEL = EnumProperty.create("panel", Finish.class, f -> f != Finish.NONE);

    protected PoweredMachineBlock(Properties properties) {
        super(properties);
    }

    public abstract Core core();

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof EnergyMachine machine)) return 0;
        final EnergyBuffer energy = machine.energy();
        if (energy.energy <= 0 || energy.capacity <= 0) return 0;
        return 1 + (int) (14L * energy.energy / energy.capacity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER, PANEL);
    }
}
