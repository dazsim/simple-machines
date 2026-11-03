package com.workshopcraft.simplemachines.neoforge;

import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class NeoTransfer {
    private static final int BUCKET = 1000;

    private NeoTransfer() {
    }

    static void install() {
        Transfer.lookup = (level, pos, side) -> level.getCapability(Capabilities.Item.BLOCK, pos, side) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK, from, fromSide);
            final ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            return ResourceHandlerUtil.moveStacking(source, target, r -> filter.test(r.toStack()), max, null);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, pos, side);
            if (target == null) return false;
            try (Transaction tx = probeTx()) {
                return target.insert(ItemResource.of(stack), 1, tx) > 0;
            }
        };
        Transfer.fluidLookup = (level, pos, side) -> level.getCapability(Capabilities.Fluid.BLOCK, pos, side) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final ResourceHandler<FluidResource> source = level.getCapability(Capabilities.Fluid.BLOCK, from, fromSide);
            final ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            ResourceStack<FluidResource> done = ResourceHandlerUtil.moveFirst(source, target, r -> filter.test(r.getFluid()), max, null);
            // cauldrons and the like only move whole buckets
            if (done == null && max < BUCKET) done = ResourceHandlerUtil.moveFirst(source, target, r -> filter.test(r.getFluid()), BUCKET, null);
            if (done == null || done.isEmpty()) return 0;
            moved.accept(done.resource().getFluid());
            return done.amount();
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
            if (target == null) return false;
            try (Transaction tx = probeTx()) {
                return target.insert(FluidResource.of(fluid), BUCKET, tx) > 0;
            }
        };
        Transfer.itemFluid = stack -> {
            final FluidStack f = FluidUtil.getFirstStackContained(stack);
            final Fluid fluid = f.isEmpty() ? null : f.getFluid();
            return fluid;
        };
        Transfer.invalidator = (level, pos) -> level.invalidateCapabilities(pos);
    }

    // probes also run from inside a move's filter, where a transaction is already open
    @SuppressWarnings("deprecation")
    private static Transaction probeTx() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }
}
