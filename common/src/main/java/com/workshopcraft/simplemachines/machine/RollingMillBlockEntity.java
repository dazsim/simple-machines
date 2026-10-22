package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.recipe.MachineInput;
import com.workshopcraft.simplemachines.recipe.MachineRecipe;
import com.workshopcraft.simplemachines.recipe.RollingRecipe;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RollingMillBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Ticking, EnergyMachine {
    public static final int CAPACITY = 10_000;
    public static final int PER_TICK = 20;
    public static final int MAX_IN = 200;
    private static final int[] IN = {0};
    private static final int[] OUT = {1};

    private final RecipeManager.CachedCheck<MachineInput, RollingRecipe> check = RecipeManager.createCheck(SimpleMachines.ROLLING);
    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, MAX_IN, 0, this::setChanged);
    private int progress;
    private int total;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> energy.energy & 0xFFFF;
                case 3 -> energy.energy >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 1 -> total = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return EnergyMenu.DATA_COUNT;
        }
    };

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.rollingMillEntity, pos, state);
    }

    @Override
    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final boolean wasLit = state.getValue(MachineBlock.LIT);
        boolean changed = false;
        final MachineInput in = new MachineInput(List.of(items.get(0)));
        final RecipeHolder<RollingRecipe> recipe = in.isEmpty() ? null : check.getRecipeFor(in, level).orElse(null);
        final ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(in);
        final ItemStack out = items.get(1);
        final boolean ready = !result.isEmpty() && (out.isEmpty()
                || ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
        boolean working = false;
        if (ready) {
            total = recipe.value().cookTime();
            if (energy.energy >= PER_TICK) {
                energy.energy -= PER_TICK;
                working = true;
                changed = true;
                if (++progress >= total) {
                    progress = 0;
                    items.get(0).shrink(recipe.value().input().count());
                    if (out.isEmpty()) items.set(1, result.copy());
                    else out.grow(result.getCount());
                }
            }
        } else if (progress != 0) {
            progress = 0;
            changed = true;
        }
        if (wasLit != working) {
            state = state.setValue(MachineBlock.LIT, working);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) setChanged(level, pos, state);
    }

    private boolean usedInRecipe(ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return false;
        for (RecipeHolder<?> holder : server.recipeAccess().getRecipes()) {
            if (holder.value().getType() == SimpleMachines.ROLLING && ((MachineRecipe) holder.value()).usesItem(stack)) return true;
        }
        return false;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        final boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(items.get(slot), stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (slot == 0 && !same) {
            progress = 0;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUT : IN;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && side != Direction.DOWN && usedInRecipe(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && side == Direction.DOWN;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        total = input.getIntOr("total", 0);
        energy.energy = Math.clamp(input.getIntOr("energy", 0), 0, CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("total", total);
        output.putInt("energy", energy.energy);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.rolling_mill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.ROLLING_MILL_MENU, id, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return 2;
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
