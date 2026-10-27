package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.pipe.FluidStore;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Burns lava from its own 4 bucket tank: 1 mB every 10 ticks. Lava buckets go in slot 0, empty buckets come out of slot 1. */
public class LavaGeneratorBlockEntity extends WorkBlockEntity implements FluidStore {
    public static final int CAPACITY = 80_000;
    public static final int PER_TICK = 80;
    public static final int STEEL_PER_TICK = 120;
    public static final int MAX_OUT = 400;
    public static final int TANK_MB = 4000;
    public static final int TICKS_PER_MB = 10;
    private static final long MB = TankBlockEntity.MB;

    private long lava;
    private int burn;

    public LavaGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.LAVA), pos, state, 2, CAPACITY, 0, MAX_OUT);
        total = TANK_MB;
    }

    @Override
    public Fluid fluid() {
        return lava > 0 ? Fluids.LAVA : Fluids.EMPTY;
    }

    @Override
    public long amount() {
        return lava;
    }

    @Override
    public long capacity() {
        return TANK_MB * MB;
    }

    @Override
    public long room(Fluid f) {
        return TankBlockEntity.still(f) == Fluids.LAVA ? capacity() - lava : 0;
    }

    @Override
    public void set(Fluid f, long a) {
        lava = TankBlockEntity.still(f) == Fluids.LAVA ? Math.max(0, Math.min(a, capacity())) : 0;
        progress = (int) (lava / MB);
    }

    @Override
    public void changed() {
        setChanged();
    }

    @Override
    public boolean drainable() {
        return false;
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == 1;
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return slot == 0 && stack.is(Items.LAVA_BUCKET);
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return false;
    }

    @Override
    public Layout layout() {
        return Layout.LAVA;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        boolean changed = false;
        final ItemStack in = items.get(0);
        if (in.is(Items.LAVA_BUCKET) && capacity() - lava >= 1000 * MB && fits(1, new ItemStack(Items.BUCKET))) {
            in.shrink(1);
            store(new ItemStack(Items.BUCKET));
            lava += 1000 * MB;
            changed = true;
        }
        final int rate = tier == Tier.IRON ? PER_TICK : STEEL_PER_TICK;
        final boolean lit = lava > 0 && energy.energy < energy.capacity;
        if (lit) {
            energy.energy = Math.min(energy.capacity, energy.energy + rate);
            if (++burn >= TICKS_PER_MB) {
                burn = 0;
                lava = Math.max(0, lava - MB);
            }
            changed = true;
        }
        final int before = energy.energy;
        energy.pushToNeighbours(level, pos);
        if (before != energy.energy) changed = true;
        progress = (int) (lava / MB);
        total = TANK_MB;
        if (state.getValue(MachineBlock.LIT) != lit) {
            state = state.setValue(MachineBlock.LIT, lit);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) setChanged(level, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lava = Math.max(0, Math.min(input.getLongOr("lava", 0L), capacity()));
        burn = input.getIntOr("lava_burn", 0);
        progress = (int) (lava / MB);
        total = TANK_MB;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("lava", lava);
        output.putInt("lava_burn", burn);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.lava_generator");
    }
}
