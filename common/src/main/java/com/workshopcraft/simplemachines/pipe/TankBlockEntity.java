package com.workshopcraft.simplemachines.pipe;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Sixteen buckets of one fluid. Amounts are droplets, 81 to the mB, so both loaders map onto it exactly. */
public class TankBlockEntity extends BlockEntity implements FluidStore {
    public static final long MB = 81, BUCKET = 1000 * MB, CAPACITY = 16 * BUCKET;

    private Fluid fluid = Fluids.EMPTY;
    private long amount;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.tankEntity, pos, state);
    }

    public Fluid fluid() {
        return fluid;
    }

    public long amount() {
        return amount;
    }

    @Override
    public long capacity() {
        return CAPACITY;
    }

    public static Fluid still(Fluid f) {
        return f instanceof FlowingFluid flowing ? flowing.getSource() : f;
    }

    /** How many droplets of f would fit right now. */
    public long room(Fluid f) {
        if (f == Fluids.EMPTY) return 0;
        return amount == 0 || still(f) == fluid ? CAPACITY - amount : 0;
    }

    /** Sets the contents without telling anyone; callers commit with changed(). */
    public void set(Fluid f, long a) {
        if (a <= 0 || f == Fluids.EMPTY) {
            fluid = Fluids.EMPTY;
            amount = 0;
        } else {
            fluid = still(f);
            amount = Math.min(a, CAPACITY);
        }
    }

    public long insert(Fluid f, long max, boolean simulate) {
        final long n = Math.min(max, room(f));
        if (n > 0 && !simulate) {
            set(f, amount + n);
            changed();
        }
        return n;
    }

    public long extract(Fluid f, long max, boolean simulate) {
        if (amount == 0 || still(f) != fluid) return 0;
        final long n = Math.min(max, amount);
        if (n > 0 && !simulate) {
            set(fluid, amount - n);
            changed();
        }
        return n;
    }

    public void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    private void read(String id, long a) {
        final Identifier key = Identifier.tryParse(id);
        set(key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(key), a);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        read(input.getStringOr("fluid", ""), input.getLongOr("amount", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (amount > 0) {
            output.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            output.putLong("amount", amount);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (amount > 0) components.set(SimpleMachines.TANK, new TankContents(fluid, amount));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        final TankContents c = components.get(SimpleMachines.TANK);
        if (c != null) set(c.fluid(), c.amount());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("fluid");
        output.discard("amount");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        if (amount > 0) {
            tag.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            tag.putLong("amount", amount);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
