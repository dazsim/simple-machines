package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** No slots: where the other pad is, what a trip costs and whether it is ready. */
public class EntangledPadMenu extends EntangledDeviceMenu {
    private final Container container;

    public static EntangledPadMenu client(int id, Inventory inventory) {
        return new EntangledPadMenu(id, new SimpleContainer(0), new SimpleContainerData(EntangledPadBlockEntity.DATA_COUNT));
    }

    public EntangledPadMenu(int id, Container container, ContainerData data) {
        super(SimpleMachines.PAD_MENU, id, data);
        checkContainerDataCount(data, EntangledPadBlockEntity.DATA_COUNT);
        this.container = container;
        addDataSlots(data);
    }

    public int status() {
        return extra(0) & 0xF;
    }

    /** 0 none, 1 overworld, 2 nether, 3 end, 4 another dimension. */
    public int dimension() {
        return extra(0) >>> 4;
    }

    public int cost() {
        return extra(1);
    }

    private int at(int i) {
        return data.get(EntangledDeviceBlockEntity.DATA_COUNT + i) & 0xFFFF;
    }

    public BlockPos target() {
        return new BlockPos(at(0) | at(1) << 16, (short) at(2), at(3) | at(4) << 16);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
