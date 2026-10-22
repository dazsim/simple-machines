package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class GeneratorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Ticking, EnergyMachine {
    public static final int CAPACITY = 40_000;
    public static final int PER_TICK = 40;
    public static final int STEEL_PER_TICK = 60;
    public static final int MAX_OUT = 200;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 0, MAX_OUT, this::setChanged);
    private int litTime;
    private int litDuration;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> energy.energy & 0xFFFF;
                case 3 -> energy.energy >>> 16;
                case 4 -> energy.capacity / 1000;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return EnergyMenu.DATA_COUNT;
        }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.generatorEntity, pos, state);
    }

    /** Solid furnace fuels only: no buckets or anything that leaves a container behind. */
    public static boolean isSolidFuel(Level level, ItemStack stack) {
        if (stack.getItem() instanceof BucketItem || stack.getItem().getCraftingRemainder() != null) return false;
        return level.fuelValues().isFuel(stack);
    }

    @Override
    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final Tier tier = state.getValue(PoweredMachineBlock.TIER);
        final int capacity = CAPACITY * tier.storage;
        energy.capacity = capacity;
        energy.maxOut = MAX_OUT * tier.storage;
        if (energy.energy > capacity) energy.energy = capacity;
        final int rate = tier == Tier.IRON ? PER_TICK : STEEL_PER_TICK;
        final boolean wasLit = litTime > 0;
        final int before = energy.energy;
        if (litTime <= 0 && energy.energy < capacity) {
            final ItemStack fuel = items.get(0);
            final int burn = isSolidFuel(level, fuel) ? level.fuelValues().burnDuration(fuel) : 0;
            if (burn > 0) {
                litTime = litDuration = burn;
                fuel.shrink(1);
            }
        }
        if (litTime > 0 && energy.energy < capacity) {
            litTime--;
            energy.energy = Math.min(capacity, energy.energy + rate);
        }
        energy.pushToNeighbours(level, pos);
        final boolean lit = litTime > 0;
        if (wasLit != lit) {
            state = state.setValue(MachineBlock.LIT, lit);
            level.setBlock(pos, state, 3);
        }
        if (wasLit != lit || before != energy.energy || lit) setChanged(level, pos, state);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level != null && isSolidFuel(level, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("lit_time", 0);
        litDuration = input.getIntOr("lit_duration", 0);
        energy.energy = Math.max(0, input.getIntOr("energy", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("lit_time", litTime);
        output.putInt("lit_duration", litDuration);
        output.putInt("energy", energy.energy);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.energy > 0) components.set(SimpleMachines.ENERGY, energy.energy);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        energy.energy = components.getOrDefault(SimpleMachines.ENERGY, 0);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("energy");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.solid_fuel_generator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.GENERATOR_MENU, id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
}
