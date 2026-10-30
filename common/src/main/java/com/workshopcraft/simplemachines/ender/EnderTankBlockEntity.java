package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.pipe.FluidStore;
import com.workshopcraft.simplemachines.pipe.TankBlockEntity;
import com.workshopcraft.simplemachines.pipe.TankContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** 16 buckets of one fluid kept on the channel. Full buckets go in slot 0 to empty, empty ones to fill; results come out of slot 1. */
public class EnderTankBlockEntity extends EnderDeviceBlockEntity implements FluidStore {
    private static final long MB = TankBlockEntity.MB, BUCKET = TankBlockEntity.BUCKET;

    public EnderTankBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.TANK), pos, state, 2, EnderData.TANK);
    }

    private TankContents contents() {
        final EnderData.Channel c = channel();
        return c == null || c.fluid == null ? new TankContents(Fluids.EMPTY, 0) : c.fluid;
    }

    @Override
    public Fluid fluid() {
        return contents().fluid();
    }

    @Override
    public long amount() {
        return contents().amount();
    }

    @Override
    public long capacity() {
        return TankBlockEntity.CAPACITY;
    }

    @Override
    public long room(Fluid f) {
        if (channel() == null || f == Fluids.EMPTY) return 0;
        final TankContents t = contents();
        return t.amount() == 0 || TankBlockEntity.still(f) == t.fluid() ? capacity() - t.amount() : 0;
    }

    @Override
    public void set(Fluid f, long a) {
        final EnderData.Channel c = channel();
        if (c == null) return;
        c.fluid = a <= 0 || f == Fluids.EMPTY ? null : new TankContents(TankBlockEntity.still(f), Math.min(a, capacity()));
    }

    @Override
    public void changed() {
        setChanged();
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot == 1;
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return slot == 0 && (stack.is(Items.BUCKET) || stack.getItem() instanceof BucketItem b && b.getContent() != Fluids.EMPTY);
    }

    @Override
    protected int extra(int i) {
        if (i == 0) return BuiltInRegistries.FLUID.getId(fluid());
        return (int) (amount() / MB);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        super.work(level, pos, state, tier);
        if (!online) return;
        final ItemStack in = items.get(0);
        if (in.getItem() instanceof BucketItem b && b.getContent() != Fluids.EMPTY) {
            if (room(b.getContent()) >= BUCKET && fits(1, new ItemStack(Items.BUCKET))) {
                final long had = amount();
                in.shrink(1);
                store(new ItemStack(Items.BUCKET));
                set(b.getContent(), had + BUCKET);
                setChanged();
            }
        } else if (in.is(Items.BUCKET) && amount() >= BUCKET && fluid().getBucket() != Items.AIR) {
            final Fluid f = fluid();
            final ItemStack full = new ItemStack(f.getBucket());
            if (fits(1, full)) {
                in.shrink(1);
                store(full);
                set(f, amount() - BUCKET);
                setChanged();
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.ender_tank");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EnderTankMenu(id, inventory, this, menuData);
    }
}
