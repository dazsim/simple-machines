package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import net.minecraft.core.Direction;
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

/** Nine ghost slots and the three filter switches for one filtered pipe end. Data: deny, tags, data. */
public class FilterMenu extends AbstractContainerMenu {
    public static final int SLOTS = PipeFilter.SIZE;
    private final SimpleContainer ghosts = new SimpleContainer(SLOTS);
    private final ContainerData data;
    private final @Nullable ConduitBlockEntity pipe;
    private final Direction side;

    public static FilterMenu client(int id, Inventory inventory) {
        return new FilterMenu(id, inventory, null, Direction.NORTH);
    }

    public FilterMenu(int id, Inventory inventory, @Nullable ConduitBlockEntity pipe, Direction side) {
        super(SimpleMachines.FILTER_MENU, id);
        this.pipe = pipe;
        this.side = side;
        if (pipe != null) for (int i = 0; i < SLOTS; i++) ghosts.setItem(i, current().item(i));
        data = pipe == null ? new SimpleContainerData(3) : new ContainerData() {
            @Override
            public int get(int i) {
                final PipeFilter f = current();
                return (i == 0 ? f.deny : i == 1 ? f.tags : f.data) ? 1 : 0;
            }

            @Override
            public void set(int i, int value) {
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new Slot(ghosts, i, 26 + i % 3 * 18, 17 + i / 3 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    private PipeFilter current() {
        final PipeFilter f = pipe == null ? null : pipe.filter(side);
        return f == null ? PipeFilter.EMPTY : f;
    }

    public boolean setting(int i) {
        return data.get(i) != 0;
    }

    private void setGhost(int slot, ItemStack stack) {
        final ItemStack one = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ghosts.setItem(slot, one);
        if (pipe != null && pipe.filter(side) != null) pipe.setFilter(side, current().withItem(slot, one));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < SLOTS) {
            if (input == ContainerInput.PICKUP) setGhost(slotId, getCarried());
            else if (input == ContainerInput.QUICK_MOVE || input == ContainerInput.THROW) setGhost(slotId, ItemStack.EMPTY);
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < SLOTS) return ItemStack.EMPTY;
        final ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int free = -1;
        for (int i = 0; i < SLOTS; i++) {
            final ItemStack g = ghosts.getItem(i);
            if (g.isEmpty()) {
                if (free < 0) free = i;
            } else if (ItemStack.isSameItemSameComponents(g, stack)) return ItemStack.EMPTY;
        }
        if (free >= 0) setGhost(free, stack);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id > 2) return false;
        if (pipe != null && pipe.filter(side) != null) pipe.setFilter(side, current().toggle(id));
        return true;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != ghosts;
    }

    @Override
    public boolean stillValid(Player player) {
        return pipe == null || !pipe.isRemoved() && pipe.filter(side) != null && Container.stillValidBlockEntity(pipe, player);
    }
}
