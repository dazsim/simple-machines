package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.cable.Conduits;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A device on an entangled channel. It shares only while it has a code and 8 FE a tick to spend. */
public abstract class EntangledDeviceBlockEntity extends WorkBlockEntity implements EntangledLink.Holder {
    public static final int CAPACITY = 20_000, MAX_IN = 200, UPKEEP = 8;
    public static final int DATA_COUNT = EnergyMenu.DATA_COUNT + 13;
    private final String kind;
    protected @Nullable EntangledLink link;
    protected boolean online;
    private @Nullable String sharedScope;
    private static final int[] NO_SLOTS = {};

    protected final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int id) {
            return id < EnergyMenu.DATA_COUNT ? data.get(id) : EntangledDeviceMenu.encode(id - EnergyMenu.DATA_COUNT, link, online, extra(0), extra(1));
        }

        @Override
        public void set(int id, int value) {
            if (id < EnergyMenu.DATA_COUNT) data.set(id, value);
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected EntangledDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, String kind) {
        this(type, pos, state, size, kind, CAPACITY, MAX_IN, 0);
    }

    protected EntangledDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, String kind, int capacity, int maxIn, int maxOut) {
        super(type, pos, state, size, capacity, maxIn, maxOut);
        this.kind = kind;
    }

    protected int extra(int i) {
        return 0;
    }

    /** The shared channel, or null while there is no code or no power. */
    protected EntangledData.@Nullable Channel channel() {
        if (link == null || !online || !(level instanceof ServerLevel server)) return null;
        return EntangledData.get(server.getServer()).channel(kind, link);
    }

    public boolean online() {
        return online;
    }

    @Override
    public @Nullable EntangledLink link() {
        return link;
    }

    @Override
    public void setLink(@Nullable EntangledLink link) {
        this.link = link;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (link != null && level instanceof ServerLevel server) EntangledData.get(server.getServer()).setDirty();
    }

    @Override
    protected boolean isOutput(int slot) {
        return false;
    }

    @Override
    public boolean energyOut(@Nullable Direction side) {
        return false;
    }

    /** Public devices work with anything; private ones only with the owner's own pipes and cables. */
    @Override
    public boolean exposed() {
        return EntangledAccess.allows(level, worldPosition, link);
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return exposed();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return exposed() ? super.getSlotsForFace(side) : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return exposed() && super.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return exposed() && super.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final String scope = link == null ? "public" : link.scope();
        if (!scope.equals(sharedScope)) {
            sharedScope = scope;
            Transfer.invalidator.invalidate(level, pos);
            Conduits.refreshAround(level, pos);
        }
        final boolean was = online;
        online = link != null && energy.energy >= upkeep();
        if (online) energy.energy -= upkeep();
        if (was != online) setChanged();
        setLit(level, pos, state, lit());
    }

    protected int upkeep() {
        return UPKEEP;
    }

    protected boolean lit() {
        return online;
    }

    @Override
    public boolean canOpen(Player player) {
        if (!EntangledLink.allows(link, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_not_owner").withStyle(ChatFormatting.RED));
            return false;
        }
        return super.canOpen(player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        link = input.read("entangled_link", EntangledLink.CODEC).orElse(null);
        online = input.getBooleanOr("online", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (link != null) output.store("entangled_link", EntangledLink.CODEC, link);
        output.putBoolean("online", online);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (link != null) components.set(SimpleMachines.ENTANGLED_LINK, link);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        link = components.get(SimpleMachines.ENTANGLED_LINK);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("entangled_link");
    }
}
