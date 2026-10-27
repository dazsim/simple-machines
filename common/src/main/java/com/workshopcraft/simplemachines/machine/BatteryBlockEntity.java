package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Stores FE. Takes it in on every side but the output sides, which push it out; the front is the output to begin with. */
public class BatteryBlockEntity extends WorkBlockEntity {
    public static final int CAPACITY = 500_000;
    public static final int RATE = 1000;
    private int lastSignal = -1;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.BATTERY), pos, state, 0, CAPACITY, RATE, RATE);
    }

    /** Capacity of a battery item, from the chassis tier it was built on. */
    public static int itemCapacity(ItemStack stack) {
        final Tier tier = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).get(PoweredMachineBlock.TIER);
        final int cells = Upgrade.count(Upgrade.parse(stack.getOrDefault(SimpleMachines.UPGRADES_FITTED, "")), Upgrade.ENERGY_CELL);
        return CAPACITY * (tier == null ? Tier.IRON : tier).storage * (1 + cells);
    }

    @Override
    protected boolean isOutput(int slot) {
        return false;
    }

    @Override
    public boolean hasOutputs() {
        return true;
    }

    @Override
    protected int defaultOutputs(BlockState state) {
        return state.hasProperty(MachineBlock.FACING) ? Ported.bit(state.getValue(MachineBlock.FACING)) : 0;
    }

    private boolean out(Direction side) {
        return (outputSides() & Ported.bit(side)) != 0;
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return side == null || !out(side);
    }

    @Override
    public boolean energyOut(@Nullable Direction side) {
        return side == null || out(side);
    }

    @Override
    public Layout layout() {
        return Layout.BATTERY;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        int left = energy.maxOut;
        for (Direction d : Direction.values()) {
            if (left <= 0 || energy.energy <= 0) break;
            if (out(d)) left -= energy.pushTo(level, pos, d, left);
        }
        final int signal = energy.energy <= 0 ? 0 : 1 + (int) (14L * energy.energy / energy.capacity);
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
        final int gauge = energy.capacity <= 0 ? 0 : (int) Math.min(5, 5L * energy.energy / energy.capacity);
        final BlockState now = getBlockState();
        if (now.hasProperty(BatteryBlock.LEVEL) && now.getValue(BatteryBlock.LEVEL) != gauge)
            level.setBlock(pos, now.setValue(BatteryBlock.LEVEL, gauge), 2);
        if (left != energy.maxOut) setChanged();
    }

    @Override
    protected void pushOut(ServerLevel level, BlockPos pos) {
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.battery");
    }
}
