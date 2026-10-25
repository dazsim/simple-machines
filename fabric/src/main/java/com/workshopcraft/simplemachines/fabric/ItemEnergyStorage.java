package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/** FE kept in the item's energy component, so a charged battery keeps its charge when placed. */
public class ItemEnergyStorage implements EnergyStorage {
    private final ContainerItemContext ctx;
    private final int capacity;
    private final int rate;

    public ItemEnergyStorage(ContainerItemContext ctx, int capacity, int rate) {
        this.ctx = ctx;
        this.capacity = capacity;
        this.rate = rate;
    }

    private boolean set(int energy, TransactionContext tx) {
        final ItemStack stack = ctx.getItemVariant().toStack();
        if (energy > 0) stack.set(SimpleMachines.ENERGY, energy);
        else stack.remove(SimpleMachines.ENERGY);
        return ctx.exchange(ItemVariant.of(stack), 1, tx) == 1;
    }

    @Override
    public long insert(long amount, TransactionContext tx) {
        if (ctx.getAmount() != 1) return 0;
        final int n = (int) Math.min(Math.min(amount, rate), capacity - getAmount());
        return n > 0 && set((int) getAmount() + n, tx) ? n : 0;
    }

    @Override
    public long extract(long amount, TransactionContext tx) {
        if (ctx.getAmount() != 1) return 0;
        final int n = (int) Math.min(Math.min(amount, rate), getAmount());
        return n > 0 && set((int) getAmount() - n, tx) ? n : 0;
    }

    @Override
    public long getAmount() {
        return ctx.getItemVariant().toStack().getOrDefault(SimpleMachines.ENERGY, 0);
    }

    @Override
    public long getCapacity() {
        return capacity;
    }
}
