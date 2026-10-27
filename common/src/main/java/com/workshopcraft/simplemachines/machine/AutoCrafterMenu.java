package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Ghost grid 0-8, result preview 9, buffer 10-18, outputs 19-22, then the player inventory. */
public class AutoCrafterMenu extends AbstractContainerMenu {
    public static final int RESULT = 9, BUFFER = 10, OUT = 19, INV = 23;
    public static final int BUTTON_CLEAR = 0, BUTTON_REDSTONE = 1, BUTTON_REDSTONE_BACK = 2, BUTTON_SET = 100;
    private final Container container;
    private final Container pattern;
    private final ContainerData data;
    private final @Nullable AutoCrafterBlockEntity machine;

    public static AutoCrafterMenu client(int id, Inventory inventory) {
        return new AutoCrafterMenu(id, inventory, new SimpleContainer(AutoCrafterBlockEntity.GRID + AutoCrafterBlockEntity.OUTPUTS),
                new SimpleContainer(AutoCrafterBlockEntity.GRID + 1), new SimpleContainerData(AutoCrafterBlockEntity.DATA_COUNT), null);
    }

    public AutoCrafterMenu(int id, Inventory inventory, Container container, Container pattern, ContainerData data, @Nullable AutoCrafterBlockEntity machine) {
        super(SimpleMachines.CRAFTER_MENU, id);
        checkContainerSize(container, AutoCrafterBlockEntity.GRID + AutoCrafterBlockEntity.OUTPUTS);
        checkContainerDataCount(data, AutoCrafterBlockEntity.DATA_COUNT);
        this.container = container;
        this.pattern = pattern;
        this.data = data;
        this.machine = machine;
        for (int i = 0; i < 9; i++) addSlot(new Ghost(pattern, i, 8 + 18 * (i % 3), 17 + 18 * (i / 3)));
        addSlot(new Ghost(pattern, RESULT, 69, 35));
        for (int i = 0; i < 9; i++) {
            final int n = i;
            addSlot(new Slot(container, i, 8 + 18 * i, 84) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return n < groups() && container.canPlaceItem(n, stack);
                }

                @Override
                public boolean isActive() {
                    return n < groups() || hasItem();
                }
            });
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(container, 9 + i, 116 + 18 * (i % 2), 26 + 18 * (i / 2)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 116);
        addDataSlots(data);
    }

    public static class Ghost extends Slot {
        public Ghost(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private void setGhost(int pos, ItemStack stack) {
        if (machine != null) machine.setGhost(pos, stack);
        else pattern.setItem(pos, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId <= RESULT) {
            if (slotId == RESULT) return;
            final ItemStack carried = getCarried();
            switch (input) {
                case PICKUP -> {
                    if (!carried.isEmpty()) setGhost(slotId, carried);
                    else if (button == 1) setGhost(slotId, ItemStack.EMPTY);
                }
                case QUICK_MOVE, THROW -> setGhost(slotId, ItemStack.EMPTY);
                default -> {}
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machine == null) return false;
        if (id == BUTTON_CLEAR) machine.clearPattern();
        else if (id == BUTTON_REDSTONE || id == BUTTON_REDSTONE_BACK) machine.cycleRedstone(id == BUTTON_REDSTONE_BACK);
        else if (id >= BUTTON_SET) {
            final int k = id - BUTTON_SET;
            final int raw = k / 9 - 1;
            machine.setGhost(k % 9, raw < 0 ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.byId(raw)));
        } else return false;
        return true;
    }

    /** Button id that sets one ghost slot, for recipe transfer. */
    public static int setButton(int pos, ItemStack stack) {
        return BUTTON_SET + pos + 9 * (stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()) + 1);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target instanceof Ghost);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof Ghost);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < BUFFER) return ItemStack.EMPTY;
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack before = stack.copy();
        if (index < INV) {
            if (!moveItemStackTo(stack, INV, INV + 36, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = BUFFER; i < OUT && !stack.isEmpty(); i++) {
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

    public int capacity() {
        return Math.max(1, data.get(4)) * 1000;
    }

    public int upgrades() {
        return data.get(6);
    }

    public int redstone() {
        return data.get(EnergyMenu.DATA_COUNT);
    }

    public int groups() {
        return data.get(EnergyMenu.DATA_COUNT + 1);
    }

    public int status() {
        return data.get(EnergyMenu.DATA_COUNT + 2);
    }

    public float energyFraction() {
        return Mth.clamp((float) energy() / capacity(), 0, 1);
    }

    public float progress() {
        final int a = data.get(0);
        final int b = data.get(1);
        return b == 0 || a == 0 ? 0 : Mth.clamp((float) a / b, 0, 1);
    }
}
