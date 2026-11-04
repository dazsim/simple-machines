package com.workshopcraft.simplemachines.forge;

import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

final class ForgeTransfer {
    private static final int BUCKET = 1000;

    private ForgeTransfer() {
    }

    static void install() {
        Transfer.lookup = (level, pos, side) -> Caps.find(level, pos, side, ForgeCapabilities.ITEM_HANDLER) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IItemHandler source = Caps.find(level, from, fromSide, ForgeCapabilities.ITEM_HANDLER);
            final IItemHandler target = Caps.find(level, to, toSide, ForgeCapabilities.ITEM_HANDLER);
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
            final IItemHandler target = Caps.find(level, pos, side, ForgeCapabilities.ITEM_HANDLER);
            return target != null && ItemHandlerHelper.insertItemStacked(target, stack.copyWithCount(1), true).isEmpty();
        };
        Transfer.fluidLookup = (level, pos, side) -> Caps.find(level, pos, side, ForgeCapabilities.FLUID_HANDLER) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IFluidHandler source = Caps.find(level, from, fromSide, ForgeCapabilities.FLUID_HANDLER);
            final IFluidHandler target = Caps.find(level, to, toSide, ForgeCapabilities.FLUID_HANDLER);
            if (source == null || target == null) return 0;
            FluidStack done = move(source, target, max, filter);
            // cauldrons and the like only move whole buckets
            if (done.isEmpty() && max < BUCKET) done = move(source, target, BUCKET, filter);
            if (done.isEmpty()) return 0;
            moved.accept(done.getFluid());
            return done.getAmount();
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final IFluidHandler target = Caps.find(level, pos, side, ForgeCapabilities.FLUID_HANDLER);
            return target != null && target.fill(new FluidStack(fluid, BUCKET), IFluidHandler.FluidAction.SIMULATE) > 0;
        };
        Transfer.itemFluid = stack -> {
            final FluidStack f = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            final Fluid fluid = f.isEmpty() ? null : f.getFluid();
            return fluid;
        };
        // providers ask the block entity on every lookup, so there is nothing cached to invalidate
        Transfer.invalidator = (level, pos) -> {
        };
    }

    private static FluidStack move(IFluidHandler source, IFluidHandler target, int max, java.util.function.Predicate<Fluid> filter) {
        for (int t = 0; t < source.getTanks(); t++) {
            final FluidStack in = source.getFluidInTank(t);
            if (in.isEmpty() || !filter.test(in.getFluid())) continue;
            final FluidStack done = FluidUtil.tryFluidTransfer(target, source, new FluidStack(in, max), true);
            if (!done.isEmpty()) return done;
        }
        return FluidStack.EMPTY;
    }
}
