package com.workshopcraft.simplemachines.compat.mc;

import com.workshopcraft.simplemachines.compat.mc.component.DataComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Routes 1.20.1 NBT save/load to the 26.x style overloads; implicit components go under SimpleMachines. */
public abstract class CompatBlockEntity extends BlockEntity implements Removal, ComponentHolder {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        loadAdditional(new ValueInput(tag, null));
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAdditional(new ValueOutput(tag, null));
        final DataComponentMap.Builder b = new DataComponentMap.Builder();
        collectImplicitComponents(b);
        if (!b.values().isEmpty()) {
            final CompoundTag data = new CompoundTag();
            Comps.write(data, b);
            tag.put(Comps.KEY, data);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return getUpdateTag((HolderLookup.Provider) null);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag();
    }

    public CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        return saveWithoutMetadata();
    }

    public CompoundTag saveCustomOnly(HolderLookup.Provider registries) {
        return saveWithoutMetadata();
    }

    public void applyComponentsFrom(DataComponentGetter components) {
        applyImplicitComponents(components);
        setChanged();
    }

    protected void loadAdditional(ValueInput input) {
    }

    protected void saveAdditional(ValueOutput output) {
    }

    protected void applyImplicitComponents(DataComponentGetter components) {
    }

    protected void collectImplicitComponents(DataComponentMap.Builder components) {
    }

    public void removeComponentsFromTag(ValueOutput output) {
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }
}
