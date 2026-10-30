package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Card slot 0, then the player inventory. A code is sent as a button id above BUTTON_SET. */
public class EnderProgrammerMenu extends AbstractContainerMenu {
    public static final int BUTTON_RANDOM = 0, BUTTON_PRIVATE = 1, BUTTON_WRITE = 2, BUTTON_SET = 1000;
    public static final int CARD_X = 120, CARD_Y = 41;
    private final Container container;
    private final ContainerData data;
    private final @Nullable EnderProgrammerBlockEntity machine;

    public static EnderProgrammerMenu client(int id, Inventory inventory) {
        return new EnderProgrammerMenu(id, inventory, new SimpleContainer(1), new SimpleContainerData(EnderProgrammerBlockEntity.DATA_COUNT), null);
    }

    public EnderProgrammerMenu(int id, Inventory inventory, Container container, ContainerData data, @Nullable EnderProgrammerBlockEntity machine) {
        super(SimpleMachines.PROGRAMMER_MENU, id);
        checkContainerSize(container, 1);
        checkContainerDataCount(data, EnderProgrammerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.machine = machine;
        addSlot(new Slot(container, 0, CARD_X, CARD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnderLink.programmable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machine == null || !(player.level() instanceof ServerLevel level)) return false;
        if (id == BUTTON_RANDOM) machine.random(level, player);
        else if (id == BUTTON_PRIVATE) machine.togglePrivate();
        else if (id == BUTTON_WRITE) machine.write(level, player);
        else if (id >= BUTTON_SET && id <= BUTTON_SET + EnderLink.MAX) machine.setCode(id - BUTTON_SET);
        else return false;
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        final ItemStack stack = slot.getItem();
        final ItemStack before = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
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
        return data.get(6) & 0xFF;
    }

    public int voltage() {
        return EnergyMenu.voltage(data.get(6));
    }

    public boolean sendsOnly() {
        return (data.get(6) & 1 << 10) != 0;
    }

    /** The stored code, or -1 when none has been set. */
    public int code() {
        return ((data.get(EnergyMenu.DATA_COUNT) & 0xFFFF) | (data.get(EnergyMenu.DATA_COUNT + 1) & 0xFFFF) << 16) - 1;
    }

    public boolean isPrivate() {
        return data.get(EnergyMenu.DATA_COUNT + 2) != 0;
    }

    public int status() {
        return data.get(EnergyMenu.DATA_COUNT + 3);
    }

    public float energyFraction() {
        return Mth.clamp((float) energy() / capacity(), 0, 1);
    }
}
