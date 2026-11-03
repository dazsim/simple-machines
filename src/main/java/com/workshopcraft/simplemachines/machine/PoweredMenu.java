package com.workshopcraft.simplemachines.machine;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menu for the machines laid out by a {@link Layout}; slots take whatever the machine accepts. */
public class PoweredMenu extends AbstractContainerMenu {
    public final Layout layout;
    private final Container container;
    private final ContainerData data;

    public PoweredMenu(MenuType<?> type, int id, Inventory inventory, Container container, ContainerData data, Layout layout) {
        super(type, id);
        this.layout = layout;
        this.container = container;
        this.data = data;
        checkContainerSize(container, layout.slots().length);
        checkContainerDataCount(data, EnergyMenu.DATA_COUNT);
        for (int i = 0; i < layout.slots().length; i++) {
            addSlot(new Slot(container, i, layout.slots()[i][0], layout.slots()[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final int invStart = container.getContainerSize();
        final ItemStack stack = slot.getItem();
        final ItemStack before = stack.copy();
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invStart + 36, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < invStart && !stack.isEmpty(); i++) {
                if (slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) moved = true;
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    public int energy() {
        return (data.get(2) & 0xFFFF) | (data.get(3) & 0xFFFF) << 16;
    }

    /** Packed upgrade counts, see Upgrade.pack. */
    public int upgrades() {
        return data.get(6) & 0xFF;
    }

    public int voltage() {
        return EnergyMenu.voltage(data.get(6));
    }

    public boolean sendsOnly() {
        return (data.get(6) & 1 << 10) != 0;
    }

    public int capacity() {
        return Math.max(1, data.get(4)) * 1000;
    }

    public float energyFraction() {
        return Mth.clamp((float) energy() / capacity(), 0, 1);
    }

    public float progress() {
        final int a = data.get(0);
        final int b = data.get(1);
        return b == 0 || a == 0 ? 0 : Mth.clamp((float) a / b, 0, 1);
    }

    public int value(int id) {
        return data.get(id);
    }
}
