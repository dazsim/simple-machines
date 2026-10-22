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

public class MachineMenu extends AbstractContainerMenu {
    public final int inputs;
    private final Container container;
    private final ContainerData data;
    private final Level level;

    public static MachineMenu alloy(int id, Inventory inventory) {
        return new MachineMenu(SimpleMachines.ALLOY_MENU, id, inventory, new SimpleContainer(4), new SimpleContainerData(MachineBlockEntity.DATA_COUNT), 2);
    }

    public static MachineMenu extruder(int id, Inventory inventory) {
        return new MachineMenu(SimpleMachines.EXTRUDER_MENU, id, inventory, new SimpleContainer(3), new SimpleContainerData(MachineBlockEntity.DATA_COUNT), 1);
    }

    public MachineMenu(MenuType<?> type, int id, Inventory inventory, Container container, ContainerData data, int inputs) {
        super(type, id);
        checkContainerSize(container, inputs + 2);
        checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);
        this.inputs = inputs;
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        if (inputs == 2) {
            addSlot(new Slot(container, 0, 47, 17));
            addSlot(new Slot(container, 1, 65, 17));
        } else {
            addSlot(new Slot(container, 0, 56, 17));
        }
        addSlot(new Slot(container, inputs, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return level.fuelValues().isFuel(stack);
            }
        });
        addSlot(new Slot(container, inputs + 1, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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
        final int fuel = inputs;
        final int out = inputs + 1;
        final int invStart = inputs + 2;
        final int invEnd = invStart + 36;
        final ItemStack stack = slot.getItem();
        final ItemStack before = stack.copy();
        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, invEnd, index == out)) return ItemStack.EMPTY;
            if (index == out) slot.onQuickCraft(stack, before);
        } else if (!(level.fuelValues().isFuel(stack) && moveItemStackTo(stack, fuel, fuel + 1, false))
                && !moveItemStackTo(stack, 0, inputs, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }

    public boolean isLit() {
        return data.get(0) > 0;
    }

    public float litProgress() {
        final int duration = data.get(1) == 0 ? 200 : data.get(1);
        return Mth.clamp((float) data.get(0) / duration, 0, 1);
    }

    public float cookProgress() {
        final int done = data.get(2);
        final int total = data.get(3);
        return total == 0 || done == 0 ? 0 : Mth.clamp((float) done / total, 0, 1);
    }
}
