package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.assembly.Worktops;
import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Shared base of the chassis-built machines: FE buffer scaled by tier, worktop, sided slots and the per-side auto-output.
 * Every face takes items into input slots and gives items from output slots.
 */
public abstract class WorkBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Ticking, EnergyMachine, Worktops.Holder, Ported {
    public static final int PUSH_INTERVAL = 8;
    public static final int PUSH_COUNT = 16;

    private final int baseCapacity;
    private final int baseIn;
    private final int baseOut;
    private final int[] slots;
    protected NonNullList<ItemStack> items;
    protected final EnergyBuffer energy;
    protected int progress;
    protected int total;
    private int outputs;
    private String worktop = Worktops.DEFAULT;

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

    protected WorkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int capacity, int maxIn, int maxOut) {
        super(type, pos, state);
        this.baseCapacity = capacity;
        this.baseIn = maxIn;
        this.baseOut = maxOut;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = i;
        this.energy = new EnergyBuffer(capacity, maxIn, maxOut, this::setChanged);
        this.outputs = defaultOutputs(state);
    }

    protected abstract boolean isOutput(int slot);

    /** Whether stack may go into input slot, checked on the server. */
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return false;
    }

    protected abstract void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier);

    protected int mode() {
        return 0;
    }

    protected int defaultOutputs(BlockState state) {
        return 0;
    }

    /** Whether a screwdriver can set sides of this machine to output. */
    public boolean hasOutputs() {
        for (int i = 0; i < items.size(); i++) if (isOutput(i)) return true;
        return false;
    }

    public boolean energyIn(@Nullable Direction side) {
        return true;
    }

    public boolean energyOut(@Nullable Direction side) {
        return true;
    }

    public Layout layout() {
        return Layout.PROCESS;
    }

    public Tier tier() {
        final BlockState state = getBlockState();
        return state.hasProperty(PoweredMachineBlock.TIER) ? state.getValue(PoweredMachineBlock.TIER) : Tier.IRON;
    }

    @Override
    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    public int outputSides() {
        return outputs;
    }

    public boolean toggleOutput(Direction side) {
        outputs ^= Ported.bit(side);
        setChanged();
        if (level instanceof ServerLevel server) {
            Transfer.invalidator.invalidate(server, getBlockPos());
            server.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        return (outputs & Ported.bit(side)) != 0;
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final Tier tier = state.getValue(PoweredMachineBlock.TIER);
        energy.capacity = baseCapacity * tier.storage;
        energy.maxIn = baseIn * tier.storage;
        energy.maxOut = baseOut * tier.storage;
        if (energy.energy > energy.capacity) energy.energy = energy.capacity;
        work(level, pos, state, tier);
        if (outputs != 0 && (level.getGameTime() + pos.hashCode()) % PUSH_INTERVAL == 0) pushOut(level, pos);
    }

    /** Auto-output: moves output slot contents into whatever sits on each output side. */
    protected void pushOut(ServerLevel level, BlockPos pos) {
        boolean any = false;
        for (int i = 0; i < items.size(); i++) if (isOutput(i) && !items.get(i).isEmpty()) any = true;
        if (!any) return;
        for (Direction d : Direction.values()) {
            if ((outputs & Ported.bit(d)) == 0) continue;
            Transfer.mover.move(level, pos, d, pos.relative(d), d.getOpposite(), PUSH_COUNT);
        }
    }

    protected boolean fits(int slot, ItemStack stack) {
        final ItemStack out = items.get(slot);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, stack) && out.getCount() + stack.getCount() <= out.getMaxStackSize();
    }

    /** Puts as much of stack as fits into the output slots, returns what is left. */
    protected ItemStack store(ItemStack stack) {
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (!isOutput(i)) continue;
            final ItemStack out = items.get(i);
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, stack)) {
                final int n = Math.min(stack.getCount(), out.getMaxStackSize() - out.getCount());
                out.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = 0; i < items.size() && !stack.isEmpty(); i++) {
            if (isOutput(i) && items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        setChanged();
        return stack;
    }

    protected boolean hasRoom() {
        for (int i = 0; i < items.size(); i++) if (isOutput(i) && items.get(i).getCount() < items.get(i).getMaxStackSize()) return true;
        return false;
    }

    protected void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        if (state.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), 3);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isOutput(slot)) return false;
        return !(level instanceof ServerLevel server) || accepts(server, slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutput(slot);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(slots.length, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        total = input.getIntOr("total", 0);
        energy.energy = Math.max(0, input.getIntOr("energy", 0));
        worktop = input.getStringOr("worktop", Worktops.DEFAULT);
        outputs = input.getIntOr("outputs", defaultOutputs(getBlockState()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("total", total);
        output.putInt("energy", energy.energy);
        output.putString("worktop", worktop);
        output.putInt("outputs", outputs);
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
        tag.putInt("outputs", outputs);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new PoweredMenu(SimpleMachines.menuFor(layout()), id, inventory, this, data, layout());
    }

    @Override
    public int getContainerSize() {
        return slots.length;
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
