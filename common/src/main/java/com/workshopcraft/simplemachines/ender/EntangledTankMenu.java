package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Bucket in and out slots, then the player inventory. Extras carry the fluid id and the amount in mB. */
public class EntangledTankMenu extends EnderDeviceMenu {
    public static final int IN_X = 40, IN_Y = 20, OUT_Y = 44;
    private final Container container;

    public static EntangledTankMenu client(int id, Inventory inventory) {
        return new EntangledTankMenu(id, inventory, new SimpleContainer(2), new SimpleContainerData(EnderDeviceBlockEntity.DATA_COUNT));
    }

    public EntangledTankMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(SimpleMachines.TANK_MENU, id, data);
        checkContainerSize(container, 2);
        this.container = container;
        addSlot(new Slot(container, 0, IN_X, IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return bucket(stack);
            }
        });
        addSlot(new Slot(container, 1, IN_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    static boolean bucket(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.getItem() instanceof BucketItem b && b.getContent() != Fluids.EMPTY;
    }

    public Fluid fluid() {
        return BuiltInRegistries.FLUID.byId(extra(0));
    }

    public int millibuckets() {
        return extra(1);
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
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (!bucket(stack) || !moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return before;
    }
}
