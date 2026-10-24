package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Machines that make something from their surroundings on a timer:
 * stone generator (water and lava beside it, makes cobblestone), water generator (two water sources beside it, fills buckets)
 * and mechanical hive (honeycomb, plus a honey bottle when given glass bottles).
 */
public class SourceBlockEntity extends WorkBlockEntity {
    public enum Kind {
        STONE(Core.STONE, 1, 20, 10),
        WATER(Core.WATER, 2, 40, 10),
        HIVE(Core.HIVE, 3, 400, 20);

        final Core core;
        final int size;
        final int time;
        final int perTick;

        Kind(Core core, int size, int time, int perTick) {
            this.core = core;
            this.size = size;
            this.time = time;
            this.perTick = perTick;
        }
    }

    public static final int CAPACITY = 10_000;
    public static final int MAX_IN = 200;
    private final Kind kind;

    public SourceBlockEntity(BlockPos pos, BlockState state, Kind kind) {
        super(SimpleMachines.MACHINE_ENTITIES.get(kind.core), pos, state, kind.size, CAPACITY, MAX_IN, 0);
        this.kind = kind;
    }

    @Override
    protected boolean isOutput(int slot) {
        return switch (kind) {
            case STONE -> true;
            case WATER -> slot == 1;
            case HIVE -> slot > 0;
        };
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        return switch (kind) {
            case STONE -> false;
            case WATER -> stack.is(Items.BUCKET);
            case HIVE -> stack.is(Items.GLASS_BOTTLE);
        };
    }

    @Override
    public boolean energyOut(Direction side) {
        return false;
    }

    @Override
    public Layout layout() {
        return switch (kind) {
            case STONE -> Layout.STONE;
            case WATER -> Layout.PROCESS;
            case HIVE -> Layout.HIVE;
        };
    }

    private int around(ServerLevel level, BlockPos pos, boolean lava, boolean sourceOnly) {
        int n = 0;
        for (Direction d : Direction.values()) {
            final FluidState f = level.getFluidState(pos.relative(d));
            if (f.is(lava ? FluidTags.LAVA : FluidTags.WATER) && (!sourceOnly || f.isSource())) n++;
        }
        return n;
    }

    private boolean ready(ServerLevel level, BlockPos pos) {
        return switch (kind) {
            case STONE -> around(level, pos, false, false) > 0 && around(level, pos, true, false) > 0 && fits(0, new ItemStack(Items.COBBLESTONE));
            case WATER -> around(level, pos, false, true) >= 2 && items.get(0).is(Items.BUCKET) && items.get(1).isEmpty();
            case HIVE -> fits(1, new ItemStack(Items.HONEYCOMB));
        };
    }

    private void make(ServerLevel level) {
        switch (kind) {
            case STONE -> store(new ItemStack(Items.COBBLESTONE));
            case WATER -> {
                items.get(0).shrink(1);
                items.set(1, new ItemStack(Items.WATER_BUCKET));
            }
            case HIVE -> {
                final ItemStack comb = items.get(1);
                if (comb.isEmpty()) items.set(1, new ItemStack(Items.HONEYCOMB));
                else comb.grow(1);
                if (items.get(0).is(Items.GLASS_BOTTLE) && fits(2, new ItemStack(Items.HONEY_BOTTLE))) {
                    items.get(0).shrink(1);
                    final ItemStack honey = items.get(2);
                    if (honey.isEmpty()) items.set(2, new ItemStack(Items.HONEY_BOTTLE));
                    else honey.grow(1);
                }
            }
        }
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        boolean working = false;
        if (ready(level, pos)) {
            total = tier.time(kind.time);
            if (energy.energy >= kind.perTick) {
                energy.energy -= kind.perTick;
                working = true;
                if (++progress >= total) {
                    progress = 0;
                    make(level);
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        setLit(level, pos, state, working);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines." + kind.core.block);
    }
}
