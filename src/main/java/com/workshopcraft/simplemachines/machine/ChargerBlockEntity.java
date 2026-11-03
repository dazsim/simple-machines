package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Fills any item that holds FE (other mods' tools and batteries), then moves it to the output slot. */
public class ChargerBlockEntity extends WorkBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_IN = 1000;
    public static final int RATE = 500;

    /** Set by the loader: the item energy API. */
    public interface ItemEnergy {
        boolean chargeable(ItemStack stack);

        /** Offers amount FE to the item in slot, returns the amount taken. */
        int insert(Container container, int slot, int amount);

        boolean full(Container container, int slot);

        /** How full the item in slot is, 0 to 1000. */
        default int charge(Container container, int slot) {
            return 0;
        }
    }

    public static ItemEnergy itemEnergy = new ItemEnergy() {
        @Override
        public boolean chargeable(ItemStack stack) {
            return false;
        }

        @Override
        public int insert(Container container, int slot, int amount) {
            return 0;
        }

        @Override
        public boolean full(Container container, int slot) {
            return true;
        }
    };

    public ChargerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.CHARGER), pos, state, 2, CAPACITY, MAX_IN, 0);
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == 1;
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return itemEnergy.chargeable(stack);
    }

    @Override
    public boolean energyOut(Direction side) {
        return false;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final ItemStack in = items.get(0);
        boolean working = false;
        if (!in.isEmpty()) {
            if (itemEnergy.full(this, 0)) {
                if (items.get(1).isEmpty()) {
                    items.set(1, in.copy());
                    items.set(0, ItemStack.EMPTY);
                    progress = 0;
                    setChanged();
                }
            } else if (energy.energy > 0) {
                final int n = itemEnergy.insert(this, 0, Math.min(energy.energy, RATE * tier.storage));
                if (n > 0) {
                    energy.energy -= n;
                    working = true;
                    setChanged();
                }
            }
        }
        total = 1000;
        progress = items.get(0).isEmpty() ? 0 : itemEnergy.charge(this, 0);
        setLit(level, pos, state, working);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.charger");
    }
}
