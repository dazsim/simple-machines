package com.workshopcraft.simplemachines.fabric;

import com.workshopcraft.simplemachines.pipe.Transfer;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/** Fabric side of the pipe hooks. Pipes count in mB, Fabric in droplets (81 per mB). */
final class FabricTransfer {
    private static final long PER_MB = FluidConstants.BUCKET / 1000;

    private FabricTransfer() {
    }

    static void install() {
        Transfer.lookup = (level, pos, side) -> present(ItemStorage.SIDED.find(level, pos, side));
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final Storage<ItemVariant> source = ItemStorage.SIDED.find(level, from, fromSide);
            final Storage<ItemVariant> target = ItemStorage.SIDED.find(level, to, toSide);
            if (source == null || target == null) return 0;
            return (int) StorageUtil.move(source, target, v -> filter.test(v.toStack()), max, null);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, side);
            return target != null && StorageUtil.simulateInsert(target, ItemVariant.of(stack), 1, Transaction.getCurrentUnsafe()) > 0;
        };
        Transfer.fluidLookup = (level, pos, side) -> present(FluidStorage.SIDED.find(level, pos, side));
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final Storage<FluidVariant> source = FluidStorage.SIDED.find(level, from, fromSide);
            final Storage<FluidVariant> target = FluidStorage.SIDED.find(level, to, toSide);
            if (source == null || target == null) return 0;
            final List<FluidVariant> held = new ArrayList<>();
            for (StorageView<FluidVariant> view : source) {
                if (!view.isResourceBlank() && view.getAmount() > 0 && !held.contains(view.getResource())) held.add(view.getResource());
            }
            for (FluidVariant v : held) {
                if (!filter.test(v.getFluid())) continue;
                long n = StorageUtil.move(source, target, v::equals, max * PER_MB, null);
                // cauldrons and the like only move whole levels
                if (n == 0 && max < 1000) n = StorageUtil.move(source, target, v::equals, FluidConstants.BUCKET, null);
                if (n > 0) {
                    moved.accept(v.getFluid());
                    return (int) Math.max(1, n / PER_MB);
                }
            }
            return 0;
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos, side);
            return target != null && StorageUtil.simulateInsert(target, FluidVariant.of(fluid), FluidConstants.BUCKET, Transaction.getCurrentUnsafe()) > 0;
        };
        Transfer.itemFluid = stack -> {
            final Storage<FluidVariant> s = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
            if (s == null) return null;
            final FluidVariant v = StorageUtil.findStoredResource(s);
            return v == null || v.isBlank() ? null : v.getFluid();
        };
    }

    // private ender devices answer strangers with the empty storage, which is not something to connect to
    private static boolean present(Storage<?> s) {
        return s != null && s != Storage.empty();
    }
}
