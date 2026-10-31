package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** No slots: the mode button, the peers on the code and the FE moving through. */
public class EntangledRelayMenu extends EntangledDeviceMenu {
    public static final int BUTTON_MODE = 0;
    private final Container container;
    private final @Nullable EntangledRelayBlockEntity relay;

    public static EntangledRelayMenu client(int id, Inventory inventory) {
        return new EntangledRelayMenu(id, new SimpleContainer(0), new SimpleContainerData(EntangledRelayBlockEntity.DATA_COUNT), null);
    }

    public EntangledRelayMenu(int id, Container container, ContainerData data, @Nullable EntangledRelayBlockEntity relay) {
        super(SimpleMachines.RELAY_MENU, id, data);
        checkContainerDataCount(data, EntangledRelayBlockEntity.DATA_COUNT);
        this.container = container;
        this.relay = relay;
        addDataSlots(data);
    }

    public boolean receiving() {
        return (extra(0) & 1) != 0;
    }

    public int peers() {
        return extra(0) >>> 1;
    }

    public int flowIn() {
        return extra(1);
    }

    public int flowOut() {
        return data.get(EntangledRelayBlockEntity.DATA_COUNT - 1) & 0xFFFF;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_MODE || relay == null) return false;
        relay.toggleMode();
        return true;
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
