package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The 27 channel slots, then the player inventory. Opened from a buffer block or from a backpack held in locked. */
public class EnderBufferMenu extends EnderDeviceMenu {
    public static final int GRID_Y = 18, INV_Y = 113, HEIGHT = 195;
    private final Container container;
    private final int locked;

    public static EnderBufferMenu client(int id, Inventory inventory) {
        return new EnderBufferMenu(id, inventory, new SimpleContainer(EnderData.SLOTS), new SimpleContainerData(EnderDeviceBlockEntity.DATA_COUNT), -1);
    }

    public EnderBufferMenu(int id, Inventory inventory, Container container, ContainerData data) {
        this(id, inventory, container, data, -1);
    }

    /** locked is the inventory slot holding the backpack that opened this, so it cannot be moved while open. */
    public EnderBufferMenu(int id, Inventory inventory, Container container, ContainerData data, int locked) {
        super(SimpleMachines.BUFFER_MENU, id, data);
        checkContainerSize(container, EnderData.SLOTS);
        this.container = container;
        this.locked = locked;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(container, c + r * 9, 8 + c * 18, GRID_Y + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return online() && EnderBackpackItem.fitsBuffer(stack) && container.canPlaceItem(getContainerSlot(), stack);
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return online();
                    }
                });
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(inventorySlot(inventory, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(inventorySlot(inventory, c, 8 + c * 18, INV_Y + 58));
        addDataSlots(data);
    }

    private Slot inventorySlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != locked;
            }
        };
    }

    @Override
    public void clicked(int slot, int button, ContainerInput type, Player player) {
        if (locked >= 0 && type == ContainerInput.SWAP && button == locked) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack before = stack.copy();
        final int n = EnderData.SLOTS;
        if (index < n) {
            if (!moveItemStackTo(stack, n, n + 36, true)) return ItemStack.EMPTY;
        } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, n, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }
}
