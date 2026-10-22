package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Generator (1 fuel slot), or Rolling Mill and Electric Furnace (input + output).
 * Data: two progress values, energy low and high 16 bits, capacity in thousands, mill mode.
 */
public class EnergyMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 6;
    public final boolean generator;
    public final boolean mill;
    private final Container container;
    private final ContainerData data;
    private final Level level;

    public static EnergyMenu generator(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.GENERATOR_MENU, id, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    public static EnergyMenu rollingMill(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.ROLLING_MILL_MENU, id, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public static EnergyMenu electricFurnace(int id, Inventory inventory) {
        return new EnergyMenu(SimpleMachines.ELECTRIC_FURNACE_MENU, id, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public EnergyMenu(MenuType<?> type, int id, Inventory inventory, Container container, ContainerData data) {
        super(type, id);
        generator = type == SimpleMachines.GENERATOR_MENU;
        mill = type == SimpleMachines.ROLLING_MILL_MENU;
        checkContainerSize(container, generator ? 1 : 2);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        if (generator) {
            addSlot(new Slot(container, 0, 80, 53) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return GeneratorBlockEntity.isSolidFuel(level, stack);
                }
            });
        } else {
            addSlot(new MachineMenu.InputSlot(container, 0, 56, 35));
            addSlot(new Slot(container, 1, 116, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
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
        } else if (generator ? !GeneratorBlockEntity.isSolidFuel(level, stack) || !moveItemStackTo(stack, 0, 1, false)
                : !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
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

    public int capacity() {
        return Math.max(1, data.get(4)) * 1000;
    }

    public boolean sheets() {
        return data.get(5) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !(container instanceof RollingMillBlockEntity be)) return false;
        be.toggleMode();
        return true;
    }

    public float energyFraction() {
        return Mth.clamp((float) energy() / capacity(), 0, 1);
    }

    /** Generator: remaining burn. Rolling Mill: work done. */
    public float progress() {
        final int a = data.get(0);
        final int b = data.get(1);
        if (generator) return a <= 0 ? 0 : Mth.clamp((float) a / (b == 0 ? 200 : b), 0, 1);
        return b == 0 || a == 0 ? 0 : Mth.clamp((float) a / b, 0, 1);
    }
}
