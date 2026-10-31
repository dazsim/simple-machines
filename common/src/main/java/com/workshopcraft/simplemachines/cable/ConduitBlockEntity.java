package com.workshopcraft.simplemachines.cable;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.PipeFilter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
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

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/** Who placed a cable or pipe, its dye colour, the sides switched off with the screwdriver, pipe filters, and the fluid a pipe shows. */
public class ConduitBlockEntity extends BlockEntity {
    private int colour = -1;
    private int blocked;
    private final Map<Direction, PipeFilter> filters = new EnumMap<>(Direction.class);
    private int filterMask;
    private Fluid fluid = Fluids.EMPTY;
    private long fluidUntil;
    private @Nullable UUID owner;

    public ConduitBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.conduitEntity, pos, state);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(@Nullable UUID who) {
        owner = who;
        setChanged();
    }

    public static @Nullable UUID ownerAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ConduitBlockEntity be ? be.owner : null;
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

    public @Nullable PipeFilter filter(Direction d) {
        return filters.get(d);
    }

    public boolean hasFilter(Direction d) {
        return (filterMask & 1 << d.get3DDataValue()) != 0;
    }

    public int filterMask() {
        return filterMask;
    }

    public void setFilter(Direction d, @Nullable PipeFilter f) {
        if (f == null) filters.remove(d);
        else filters.put(d, f);
        filterMask = mask(filters);
        changed();
    }

    /** Takes the filter off a side and returns it as an item, or empty if there was none. */
    public ItemStack removeFilter(Direction d) {
        final PipeFilter f = filters.get(d);
        if (f == null) return ItemStack.EMPTY;
        setFilter(d, null);
        return filterItem(f);
    }

    public static ItemStack filterItem(PipeFilter f) {
        final ItemStack stack = new ItemStack(SimpleMachines.pipeFilter);
        if (!f.equals(PipeFilter.EMPTY)) stack.set(SimpleMachines.FILTER, f);
        return stack;
    }

    private static int mask(Map<Direction, PipeFilter> map) {
        int m = 0;
        for (Direction d : map.keySet()) m |= 1 << d.get3DDataValue();
        return m;
    }

    public Fluid shownFluid(long gameTime) {
        return gameTime < fluidUntil ? fluid : Fluids.EMPTY;
    }

    /** Shows a fluid in the pipe window for a while; only resends when it changes or is about to run out. */
    public void showFluid(Fluid f, long gameTime, int ticks) {
        if (f == fluid && fluidUntil - gameTime > ticks / 2) return;
        fluid = f;
        fluidUntil = gameTime + ticks;
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        for (PipeFilter f : filters.values()) Block.popResource(level, pos, filterItem(f));
        filters.clear();
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
        filters.clear();
        for (Direction d : Direction.values()) input.read("filter_" + d.getSerializedName(), PipeFilter.CODEC).ifPresent(f -> filters.put(d, f));
        filterMask = input.getIntOr("filters", mask(filters));
        final Identifier id = Identifier.tryParse(input.getStringOr("fluid", ""));
        fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(id);
        fluidUntil = input.getLongOr("fluid_until", 0);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("colour", colour);
        output.putInt("blocked", blocked);
        filters.forEach((d, f) -> output.store("filter_" + d.getSerializedName(), PipeFilter.CODEC, f));
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("colour", colour);
        tag.putInt("blocked", blocked);
        tag.putInt("filters", filterMask);
        if (fluid != Fluids.EMPTY) {
            tag.putString("fluid", BuiltInRegistries.FLUID.getKey(fluid).toString());
            tag.putLong("fluid_until", fluidUntil);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
