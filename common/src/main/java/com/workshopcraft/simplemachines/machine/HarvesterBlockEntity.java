package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Walks the square in front, harvests ripe crops (anything built on CropBlock) and plants them again. */
public class HarvesterBlockEntity extends AreaBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_IN = 200;
    public static final int PER_CROP = 400;
    public static final int STEP = 4;
    private int cursor;

    public HarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.HARVESTER), pos, state, CAPACITY, MAX_IN);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        if (progress > 0) progress--;
        setLit(level, pos, state, progress > 0);
        if (level.getGameTime() % tier.time(STEP) != 0 || energy.energy < PER_CROP || !hasRoom()) return;
        final int r = radius(tier);
        final int side = 2 * r + 1;
        final BlockPos c = centre(state, tier);
        total = 20;
        for (int tries = 0; tries < side; tries++) {
            cursor = Math.floorMod(cursor + 1, side * side);
            final BlockPos p = c.offset(cursor % side - r, 0, cursor / side - r);
            final BlockState crop = level.getBlockState(p);
            if (!(crop.getBlock() instanceof CropBlock block) || !block.isMaxAge(crop)) continue;
            final List<ItemStack> drops = Block.getDrops(crop, level, p, null);
            final ItemStack seed = crop.getCloneItemStack(level, p, false);
            for (ItemStack d : drops) {
                if (!seed.isEmpty() && ItemStack.isSameItem(d, seed)) {
                    d.shrink(1);
                    break;
                }
            }
            level.setBlock(p, block.getStateForAge(0), 3);
            level.levelEvent(2001, p, Block.getId(crop));
            for (ItemStack d : drops) if (!d.isEmpty()) keep(level, d);
            energy.energy -= PER_CROP;
            progress = total;
            setChanged();
            return;
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.crop_harvester");
    }
}
