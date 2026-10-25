package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Shears anything shearable in the square in front and collects what falls off. */
public class ShearerBlockEntity extends AreaBlockEntity {
    public static final int CAPACITY = 20_000;
    public static final int MAX_IN = 200;
    public static final int PER_SHEAR = 500;
    public static final int INTERVAL = 40;

    public ShearerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.SHEARER), pos, state, CAPACITY, MAX_IN);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        if (progress > 0) progress--;
        setLit(level, pos, state, progress > 0);
        if (!due(tier.time(INTERVAL)) || energy.energy < PER_SHEAR || !hasRoom()) return;
        final AABB box = area(state, tier, 3);
        final List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Shearable s && s.readyForShearing() && m.isAlive());
        if (mobs.isEmpty()) return;
        final Mob mob = mobs.getFirst();
        final AABB around = mob.getBoundingBox().inflate(1.5);
        final Set<UUID> before = new HashSet<>();
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, around)) before.add(e.getUUID());
        ((Shearable) mob).shear(level, SoundSource.BLOCKS, new ItemStack(Items.SHEARS));
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, around)) {
            if (before.contains(e.getUUID()) || !e.isAlive()) continue;
            keep(level, e.getItem().copy());
            e.discard();
        }
        energy.energy -= PER_SHEAR;
        total = 20;
        progress = total;
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.shearer");
    }
}
