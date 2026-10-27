package com.workshopcraft.simplemachines.gen;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SolarPanelBlockEntity extends BlockEntity {
    public static final int[] DAY = {4, 20, 100, 1000};
    public static final int[] NIGHT = {0, 0, 0, 500};
    public static final int[] BUFFER = {2000, 10000, 50000, 500000};
    public static final int[] OUT = {64, 256, 1000, 8000};
    public static final int[] VOLTAGE = {Voltage.LV, Voltage.LV, Voltage.LV, Voltage.MV};
    public final int tier;
    public final EnergyBuffer energy;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.solarEntity, pos, state);
        tier = state.getBlock() instanceof SolarPanelBlock b ? b.tier : 1;
        energy = new EnergyBuffer(BUFFER[tier - 1], 0, OUT[tier - 1], this::setChanged);
    }

    public int making(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) return 0;
        return level.isBrightOutside() ? DAY[tier - 1] : level.dimensionType().hasFixedTime() ? 0 : NIGHT[tier - 1];
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final int n = Math.min(making(level, pos), energy.capacity - energy.energy);
        if (n > 0) {
            energy.energy += n;
            setChanged();
        }
        Generators.pushOut(level, pos, energy, VOLTAGE[tier - 1], Direction.UP);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.energy = Math.min(energy.capacity, input.getIntOr("energy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy.energy);
    }
}
