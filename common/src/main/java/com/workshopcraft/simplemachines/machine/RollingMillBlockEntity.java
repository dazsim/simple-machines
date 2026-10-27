package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.recipe.RollingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Rolls ingots into rods, or plates into casings, picked with the button in its screen.
 * A mill saved in the removed sheet mode stays locked until its mode is changed and both slots are empty.
 */
public class RollingMillBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 10_000;
    public static final int PER_TICK = 20;
    public static final int MAX_IN = 200;

    public static final int RODS = 0, CASINGS = 1, SHEETS = 2, LOCKED = 4;

    private int mode = RODS;
    private boolean locked;

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.rollingMillEntity, pos, state, CAPACITY, MAX_IN, PER_TICK);
    }

    @Override
    protected int mode() {
        return mode | (locked ? LOCKED : 0);
    }

    public void toggleMode() {
        mode = mode == RODS ? CASINGS : RODS;
        forget();
        setChanged();
    }

    public boolean locked() {
        return locked;
    }

    private String recipeMode() {
        return mode == CASINGS ? RollingRecipe.CASINGS : mode == SHEETS ? RollingRecipe.SHEETS : RollingRecipe.RODS;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, com.workshopcraft.simplemachines.assembly.Tier tier) {
        if (locked) {
            if (mode == SHEETS || !items.get(0).isEmpty() || !items.get(1).isEmpty()) {
                setLit(level, pos, state, false);
                return;
            }
            locked = false;
            setChanged();
        }
        super.work(level, pos, state, tier);
    }

    @Override
    protected @Nullable Job find(ServerLevel level, ItemStack input) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof RollingRecipe r && r.mode().equals(recipeMode()) && r.input().ingredient().test(input)) {
                return new Job(r.result().create(), r.input().count(), r.cookTime());
            }
        }
        return null;
    }

    @Override
    protected boolean accepts(ServerLevel level, ItemStack stack) {
        if (locked) return false;
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof RollingRecipe r && r.usesItem(stack)) return true;
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        final boolean legacy = input.getBooleanOr("sheets", false);
        mode = input.getIntOr("roll_mode", legacy ? SHEETS : RODS);
        if (mode < RODS || mode > SHEETS) mode = RODS;
        locked = input.getBooleanOr("locked", mode == SHEETS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("roll_mode", mode);
        output.putBoolean("locked", locked);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.rolling_mill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.ROLLING_MILL_MENU, id, inventory, this, data);
    }
}
