package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

final class NeoTransfer {
    private static final int BUCKET = 1000;

    private NeoTransfer() {
    }

    static void install() {
        Transfer.lookup = (level, pos, side) -> level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, from, fromSide);
            final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            int left = max;
            for (int slot = 0; slot < source.getSlots() && left > 0; slot++) {
                final ItemStack offer = source.extractItem(slot, left, true);
                if (offer.isEmpty() || !filter.test(offer)) continue;
                final int n = offer.getCount() - ItemHandlerHelper.insertItemStacked(target, offer, true).getCount();
                if (n <= 0) continue;
                final ItemStack taken = source.extractItem(slot, n, false);
                final ItemStack back = ItemHandlerHelper.insertItemStacked(target, taken, false);
                if (!back.isEmpty()) source.insertItem(slot, back, false);
                left -= taken.getCount() - back.getCount();
            }
            return max - left;
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            return target != null && ItemHandlerHelper.insertItemStacked(target, stack.copyWithCount(1), true).isEmpty();
        };
        Transfer.fluidLookup = (level, pos, side) -> level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, from, fromSide);
            final IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            FluidStack done = move(source, target, max, filter);
            // cauldrons and the like only move whole buckets
            if (done.isEmpty() && max < BUCKET) done = move(source, target, BUCKET, filter);
            if (done.isEmpty()) return 0;
            moved.accept(done.getFluid());
            return done.getAmount();
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            return target != null && target.fill(new FluidStack(fluid, BUCKET), IFluidHandler.FluidAction.SIMULATE) > 0;
        };
        Transfer.itemFluid = stack -> {
            final FluidStack f = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            final Fluid fluid = f.isEmpty() ? null : f.getFluid();
            return fluid;
        };
        Transfer.invalidator = (level, pos) -> level.invalidateCapabilities(pos);
    }

    private static FluidStack move(IFluidHandler source, IFluidHandler target, int max, java.util.function.Predicate<Fluid> filter) {
        for (int t = 0; t < source.getTanks(); t++) {
            final FluidStack in = source.getFluidInTank(t);
            if (in.isEmpty() || !filter.test(in.getFluid())) continue;
            final FluidStack done = FluidUtil.tryFluidTransfer(target, source, in.copyWithAmount(max), true);
            if (!done.isEmpty()) return done;
        }
        return FluidStack.EMPTY;
    }
}
