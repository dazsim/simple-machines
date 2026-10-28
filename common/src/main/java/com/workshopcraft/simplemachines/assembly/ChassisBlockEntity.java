package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Holds the loose worktop sitting on a chassis, which can come from any mod, and the upgrades fitted so far. */
public class ChassisBlockEntity extends BlockEntity implements Worktops.Holder, Upgrade.Holder {
    private String worktop = "";
    private List<Upgrade> upgrades = new ArrayList<>();
    private @Nullable Upgrade loose;
    private String drill = "";

    public ChassisBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.chassisEntity, pos, state);
    }

    @Override
    public String worktop() {
        return worktop;
    }

    @Override
    public void setWorktop(String id) {
        worktop = id;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public List<Upgrade> upgrades() {
        return upgrades;
    }

    @Override
    public void setUpgrades(List<Upgrade> list) {
        upgrades = new ArrayList<>(list);
        changed();
    }

    public @Nullable Upgrade loose() {
        return loose;
    }

    public void setLoose(@Nullable Upgrade upgrade) {
        loose = upgrade;
        changed();
    }

    /** The quarry drill riding along with a fitted quarry assembly. */
    public String drill() {
        return drill;
    }

    public void setDrill(String data) {
        drill = data == null ? "" : data;
        setChanged();
    }

    /** Screws the loose upgrade in. */
    public void fix() {
        if (loose == null) return;
        upgrades.add(loose);
        loose = null;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        worktop = input.getStringOr("worktop", "");
        upgrades = Upgrade.parse(input.getStringOr("upgrades", ""));
        loose = Upgrade.byId(input.getStringOr("loose_upgrade", ""));
        drill = input.getStringOr("drill", "");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("worktop", worktop);
        output.putString("upgrades", Upgrade.join(upgrades));
        output.putString("loose_upgrade", loose == null ? "" : loose.id);
        output.putString("drill", drill);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("worktop", worktop);
        tag.putString("upgrades", Upgrade.join(upgrades));
        tag.putString("loose_upgrade", loose == null ? "" : loose.id);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
