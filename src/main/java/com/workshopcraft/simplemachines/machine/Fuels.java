package com.workshopcraft.simplemachines.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
//? if >=26.3 {
/*import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
*///?}

/** Furnace fuel lookups. 26.3 moved burn times from level fuel values to a data driven item component. */
public final class Fuels {
    private Fuels() {}

    public static boolean isFuel(Level level, ItemStack stack) {
        //? if >=26.3 {
        /*return stack.has(DataComponents.COOKING_FUEL);
        *///?} else if >=26.1 {
        return level.fuelValues().isFuel(stack);
        //?} else {
        /*return burnTime(stack) > 0;
        *///?}
    }

    /** Burn ticks for one item used by the given machine. */
    public static int burn(ServerLevel level, ItemStack stack, BaseContainerBlockEntity be) {
        //? if >=26.3 {
        /*if (!stack.has(DataComponents.COOKING_FUEL)) return 0;
        final LootContext ctx = new LootContext.Builder(new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, be.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, be)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(be.getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, be)
                .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, ctx, 0);
        *///?} else if >=26.1 {
        return level.fuelValues().burnDuration(stack);
        //?} else {
        /*return burnTime(stack);
        *///?}
    }

    //? if <26.1 {
    /*private static int burnTime(ItemStack stack) {
        //? if neoforge || forge {
        return stack.getBurnTime(null);
        //?} else {
        /^Integer t = net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.get(stack.getItem());
        return t == null ? 0 : t;
        ^///?}
    }
    *///?}
}
