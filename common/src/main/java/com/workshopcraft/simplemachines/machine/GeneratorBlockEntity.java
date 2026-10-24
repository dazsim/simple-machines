package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class GeneratorBlockEntity extends WorkBlockEntity {
    public static final int CAPACITY = 40_000;
    public static final int PER_TICK = 40;
    public static final int STEEL_PER_TICK = 60;
    public static final int MAX_OUT = 200;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.generatorEntity, pos, state, 1, CAPACITY, 0, MAX_OUT);
    }

    /** Solid furnace fuels only: no buckets or anything that leaves a container behind. */
    public static boolean isSolidFuel(Level level, ItemStack stack) {
        if (stack.getItem() instanceof BucketItem || stack.getItem().getCraftingRemainder() != null) return false;
        return level.fuelValues().isFuel(stack);
    }

    @Override
    protected boolean isOutput(int slot) {
        return false;
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return isSolidFuel(level, stack);
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return false;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final int capacity = energy.capacity;
        final int rate = tier == Tier.IRON ? PER_TICK : STEEL_PER_TICK;
        final boolean wasLit = progress > 0;
        final int before = energy.energy;
        if (progress <= 0 && energy.energy < capacity) {
            final ItemStack fuel = items.get(0);
            final int burn = isSolidFuel(level, fuel) ? level.fuelValues().burnDuration(fuel) : 0;
            if (burn > 0) {
                progress = total = burn;
                fuel.shrink(1);
            }
        }
        if (progress > 0 && energy.energy < capacity) {
            progress--;
            energy.energy = Math.min(capacity, energy.energy + rate);
        }
        energy.pushToNeighbours(level, pos);
        final boolean lit = progress > 0;
        if (wasLit != lit) {
            state = state.setValue(MachineBlock.LIT, lit);
            level.setBlock(pos, state, 3);
        }
        if (wasLit != lit || before != energy.energy || lit) setChanged(level, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("lit_time", progress);
        total = input.getIntOr("lit_duration", total);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("lit_time", progress);
        output.putInt("lit_duration", total);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.solid_fuel_generator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.GENERATOR_MENU, id, inventory, this, data);
    }
}
