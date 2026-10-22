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

/** Rolls ingots into rods or sheets, picked with the button in its screen. */
public class RollingMillBlockEntity extends PoweredBlockEntity {
    public static final int CAPACITY = 10_000;
    public static final int PER_TICK = 20;
    public static final int MAX_IN = 200;

    private boolean sheets;

    public RollingMillBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.rollingMillEntity, pos, state, CAPACITY, MAX_IN, PER_TICK);
    }

    @Override
    protected int mode() {
        return sheets ? 1 : 0;
    }

    public void toggleMode() {
        sheets = !sheets;
        forget();
        setChanged();
    }

    @Override
    protected @Nullable Job find(ServerLevel level, ItemStack input) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof RollingRecipe r && r.sheet() == sheets && r.input().ingredient().test(input)) {
                return new Job(r.result().create(), r.input().count(), r.cookTime());
            }
        }
        return null;
    }

    @Override
    protected boolean accepts(ServerLevel level, ItemStack stack) {
        for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
            if (holder.value() instanceof RollingRecipe r && r.usesItem(stack)) return true;
        }
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        sheets = input.getBooleanOr("sheets", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("sheets", sheets);
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
