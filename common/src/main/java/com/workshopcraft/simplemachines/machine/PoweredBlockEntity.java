package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import com.workshopcraft.simplemachines.assembly.Worktops;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/** Single input, single output machine that runs on FE. Speed and storage follow the chassis tier. */
public abstract class PoweredBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Ticking, EnergyMachine, Worktops.Holder {
    private static final int[] IN = {0};
    private static final int[] OUT = {1};

    protected record Job(ItemStack result, int count, int time) {
    }

    private final int baseCapacity;
    private final int baseIn;
    private final int perTick;
    protected NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    protected final EnergyBuffer energy;
    protected int progress;
    protected int total;
    private ItemStack cachedFor = ItemStack.EMPTY;
    private @Nullable Job cached;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> energy.energy & 0xFFFF;
                case 3 -> energy.energy >>> 16;
                case 4 -> energy.capacity / 1000;
                case 5 -> mode();
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 1 -> total = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return EnergyMenu.DATA_COUNT;
        }
    };

    protected PoweredBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxIn, int perTick) {
        super(type, pos, state);
        this.baseCapacity = capacity;
        this.baseIn = maxIn;
        this.perTick = perTick;
        this.energy = new EnergyBuffer(capacity, maxIn, 0, this::setChanged);
    }

    protected abstract @Nullable Job find(ServerLevel level, ItemStack input);

    protected abstract boolean accepts(ServerLevel level, ItemStack stack);

    protected int mode() {
        return 0;
    }

    protected void forget() {
        cachedFor = ItemStack.EMPTY;
        cached = null;
        progress = 0;
    }

    @Override
    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final Tier tier = state.getValue(PoweredMachineBlock.TIER);
        energy.capacity = baseCapacity * tier.storage;
        energy.maxIn = baseIn * tier.storage;
        if (energy.energy > energy.capacity) energy.energy = energy.capacity;

        final boolean wasLit = state.getValue(MachineBlock.LIT);
        boolean changed = false;
        final ItemStack in = items.get(0);
        if (!ItemStack.matches(in, cachedFor)) {
            cachedFor = in.copy();
            cached = in.isEmpty() ? null : find(level, in);
        }
        final Job job = cached;
        final ItemStack out = items.get(1);
        final boolean ready = job != null && in.getCount() >= job.count() && (out.isEmpty()
                || ItemStack.isSameItemSameComponents(out, job.result()) && out.getCount() + job.result().getCount() <= out.getMaxStackSize());
        boolean working = false;
        if (ready) {
            total = tier.time(job.time());
            if (energy.energy >= perTick) {
                energy.energy -= perTick;
                working = true;
                changed = true;
                if (++progress >= total) {
                    progress = 0;
                    in.shrink(job.count());
                    if (out.isEmpty()) items.set(1, job.result().copy());
                    else out.grow(job.result().getCount());
                }
            }
        } else if (progress != 0) {
            progress = 0;
            changed = true;
        }
        if (wasLit != working) {
            state = state.setValue(MachineBlock.LIT, working);
            level.setBlock(pos, state, 3);
            changed = true;
        }
        if (changed) setChanged(level, pos, state);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        final boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(items.get(slot), stack);
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        if (slot == 0 && !same) {
            progress = 0;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0) return false;
        return !(level instanceof ServerLevel server) || accepts(server, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUT : IN;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 && side == Direction.DOWN;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        total = input.getIntOr("total", 0);
        energy.energy = Math.max(0, input.getIntOr("energy", 0));
        worktop = input.getStringOr("worktop", Worktops.DEFAULT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("total", total);
        output.putInt("energy", energy.energy);
        output.putString("worktop", worktop);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (energy.energy > 0) components.set(SimpleMachines.ENERGY, energy.energy);
        components.set(SimpleMachines.WORKTOP, worktop);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        energy.energy = components.getOrDefault(SimpleMachines.ENERGY, 0);
        worktop = components.getOrDefault(SimpleMachines.WORKTOP, Worktops.DEFAULT);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("energy");
        output.discard("worktop");
    }

    private String worktop = Worktops.DEFAULT;

    @Override
    public String worktop() {
        return worktop;
    }

    @Override
    public void setWorktop(String id) {
        worktop = id;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("worktop", worktop);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }
}
