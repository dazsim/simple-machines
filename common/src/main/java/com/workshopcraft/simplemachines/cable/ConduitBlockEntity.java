package com.workshopcraft.simplemachines.cable;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Dye colour of a cable or pipe, and the cable sides switched off with the screwdriver. Colour stays in the world only. */
public class ConduitBlockEntity extends BlockEntity {
    private int colour = -1;
    private int blocked;

    public ConduitBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.conduitEntity, pos, state);
    }

    public @Nullable DyeColor colour() {
        return colour < 0 ? null : DyeColor.byId(colour);
    }

    public void setColour(@Nullable DyeColor c) {
        colour = c == null ? -1 : c.getId();
        changed();
    }

    public boolean blocked(Direction d) {
        return (blocked & 1 << d.get3DDataValue()) != 0;
    }

    /** Flips a side and returns whether it is now connected. */
    public boolean toggle(Direction d) {
        blocked ^= 1 << d.get3DDataValue();
        changed();
        return !blocked(d);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    public static @Nullable DyeColor colourAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ConduitBlockEntity be ? be.colour() : null;
    }

    public static boolean blockedAt(BlockGetter level, BlockPos pos, Direction d) {
        return level.getBlockEntity(pos) instanceof ConduitBlockEntity be && be.blocked(d);
    }

    /** Whether two neighbouring conduits of the same kind join: no side switched off and colours that match or are blank. */
    public static boolean joins(BlockGetter level, BlockPos pos, Direction d) {
        final BlockPos n = pos.relative(d);
        if (blockedAt(level, pos, d) || blockedAt(level, n, d.getOpposite())) return false;
        final DyeColor a = colourAt(level, pos), b = colourAt(level, n);
        return a == null || b == null || a == b;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        colour = input.getIntOr("colour", -1);
        blocked = input.getIntOr("blocked", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("colour", colour);
        output.putInt("blocked", blocked);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("colour", colour);
        tag.putInt("blocked", blocked);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
