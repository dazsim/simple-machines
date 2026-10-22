package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.recipe.CountedIngredient;
import com.workshopcraft.simplemachines.recipe.MachineInput;
import com.workshopcraft.simplemachines.recipe.MachineRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Fuel-burning machine: input slots, then one fuel slot, then one output slot. */
public abstract class MachineBlockEntity<R extends MachineRecipe> extends BaseContainerBlockEntity implements WorldlyContainer, Ticking {
    public static final int DATA_COUNT = 4;

    protected final int inputs;
    private final int fuelSlot;
    private final int outSlot;
    private final int[] top;
    private final int[] sides;
    private final int[] bottom;
    private final RecipeType<R> type;
    private final RecipeManager.CachedCheck<MachineInput, R> check;
    private NonNullList<ItemStack> items;
    private int litTime;
    private int litDuration;
    private int cookTime;
    private int cookTotal;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookTime;
                case 3 -> cookTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookTime = value;
                case 3 -> cookTotal = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected MachineBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state, int inputs, RecipeType<R> type) {
        super(beType, pos, state);
        this.inputs = inputs;
        this.fuelSlot = inputs;
        this.outSlot = inputs + 1;
        this.type = type;
        this.check = RecipeManager.createCheck(type);
        this.items = NonNullList.withSize(inputs + 2, ItemStack.EMPTY);
        top = new int[inputs];
        sides = new int[inputs + 1];
        sides[0] = fuelSlot;
        for (int i = 0; i < inputs; i++) {
            top[i] = i;
            sides[i + 1] = i;
        }
        bottom = new int[]{outSlot};
    }

    protected ContainerData data() {
        return data;
    }

    private MachineInput input() {
        return new MachineInput(new ArrayList<>(items.subList(0, inputs)));
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final boolean wasLit = litTime > 0;
        boolean changed = false;
        if (litTime > 0) litTime--;

        final MachineInput in = input();
        final RecipeHolder<R> recipe = in.isEmpty() ? null : check.getRecipeFor(in, level).orElse(null);
        final ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(in);
        final boolean ready = !result.isEmpty() && fits(result);

        if (ready && litTime <= 0) {
            final ItemStack fuel = items.get(fuelSlot);
            final int burn = level.fuelValues().burnDuration(fuel);
            if (burn > 0) {
                litTime = litDuration = burn;
                final Item fuelItem = fuel.getItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    final ItemStackTemplate left = fuelItem.getCraftingRemainder();
                    items.set(fuelSlot, left != null ? left.create() : ItemStack.EMPTY);
                }
                changed = true;
            }
        }

        if (ready) {
            cookTotal = recipe.value().cookTime();
            if (litTime > 0) {
                if (++cookTime >= cookTotal) {
                    cookTime = 0;
                    final List<CountedIngredient> used = recipe.value().matched(in);
                    for (int i = 0; i < used.size(); i++) items.get(i).shrink(used.get(i).count());
                    final ItemStack out = items.get(outSlot);
                    if (out.isEmpty()) items.set(outSlot, result.copy());
                    else out.grow(result.getCount());
                    changed = true;
                }
            } else if (cookTime > 0) {
                cookTime = Mth.clamp(cookTime - 2, 0, cookTotal);
            }
        } else {
            cookTime = 0;
        }

        final boolean lit = litTime > 0;
        if (wasLit != lit) {
            state = state.setValue(MachineBlock.LIT, lit);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) setChanged(level, pos, state);
    }

    private boolean fits(ItemStack result) {
        final ItemStack out = items.get(outSlot);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    /** Hopper rule: an input slot takes an item only if no other input slot already holds it, so one input cannot block the other. */
    private boolean acceptsInput(int slot, ItemStack stack) {
        if (!(level instanceof ServerLevel server)) return false;
        final ItemStack here = items.get(slot);
        if (!here.isEmpty()) return ItemStack.isSameItemSameComponents(here, stack);
        for (int i = 0; i < inputs; i++) {
            if (i != slot && ItemStack.isSameItemSameComponents(items.get(i), stack)) return false;
        }
        for (RecipeHolder<?> holder : server.recipeAccess().getRecipes()) {
            if (holder.value().getType() == type && ((MachineRecipe) holder.value()).usesItem(stack)) return true;
        }
        return false;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        final boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(items.get(slot), stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (slot < inputs && !same) {
            cookTime = 0;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == outSlot) return false;
        if (slot == fuelSlot) return level != null && level.fuelValues().isFuel(stack);
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? top : side == Direction.DOWN ? bottom : sides;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (side == Direction.DOWN || slot == outSlot) return false;
        if (slot == fuelSlot) return side != Direction.UP && canPlaceItem(slot, stack);
        return acceptsInput(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot == outSlot;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(inputs + 2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("lit_time", 0);
        litDuration = input.getIntOr("lit_duration", 0);
        cookTime = input.getIntOr("cook_time", 0);
        cookTotal = input.getIntOr("cook_total", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("lit_time", litTime);
        output.putInt("lit_duration", litDuration);
        output.putInt("cook_time", cookTime);
        output.putInt("cook_total", cookTotal);
    }

    @Override
    public int getContainerSize() {
        return inputs + 2;
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
