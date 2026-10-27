package com.workshopcraft.simplemachines.gen;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Lives in the lower half. Output = 50 x height x openness x weather, measured at the top half. */
public class WindTurbineBlockEntity extends BlockEntity {
    public static final int MAX = 50;
    public final EnergyBuffer energy = new EnergyBuffer(20000, 0, 200, this::setChanged);
    public float openness = -1;
    private float part;
    private int age;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.windEntity, pos, state);
    }

    public static float height(BlockPos base) {
        return Mth.clamp((base.getY() + 1 - 64) / 96F, 0F, 1F);
    }

    public static float weather(Level level) {
        return level.isThundering() ? 1.5F : level.isRaining() ? 1.25F : 1F;
    }

    /** Air share of the 9x9x9 cube around the top half, not counting the turbine itself. */
    public static float measure(Level level, BlockPos base) {
        final BlockPos top = base.above();
        int air = 0, all = 0;
        final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    p.set(top.getX() + dx, top.getY() + dy, top.getZ() + dz);
                    if (p.equals(top) || p.equals(base)) continue;
                    all++;
                    if (level.getBlockState(p).isAir()) air++;
                }
            }
        }
        return air / (float) all;
    }

    public float making(Level level, BlockPos pos) {
        if (openness < 0) openness = measure(level, pos);
        return MAX * height(pos) * openness * weather(level);
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (age++ % 100 == 0) openness = measure(level, pos);
        part += making(level, pos);
        final int n = (int) part;
        part -= n;
        final int add = Math.min(n, energy.capacity - energy.energy);
        if (add > 0) {
            energy.energy += add;
            setChanged();
        }
        Generators.pushOut(level, pos, energy, Voltage.LV, Direction.UP);
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
