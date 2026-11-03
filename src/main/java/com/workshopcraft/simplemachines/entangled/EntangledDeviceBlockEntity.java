package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.ServerConfig;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.cable.Conduits;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A device on an entangled channel. It shares only while it has a code and 8 FE a tick to spend. */
public abstract class EntangledDeviceBlockEntity extends WorkBlockEntity implements EntangledLink.Holder {
    public static final int CAPACITY = 20_000, MAX_IN = 200, UPKEEP = 8;
    public static final int LOADER_DATA = 13, DATA_COUNT = EnergyMenu.DATA_COUNT + LOADER_DATA + 5;
    /** Ticks a running loader keeps its chunks without power, and how many ticks of draw it needs banked to start again. */
    public static final int GRACE = 200, RESTART_TICKS = 20;
    private final String kind;
    private boolean loaderOn = true, powerDown;
    private int grace, draw, radius = -1;
    private LoaderStatus loader = LoaderStatus.NONE;
    protected @Nullable EntangledLink link;
    protected boolean online;
    private @Nullable String sharedScope;
    private static final int[] NO_SLOTS = {};

    protected final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int id) {
            if (id < EnergyMenu.DATA_COUNT) return data.get(id);
            final int k = id - EnergyMenu.DATA_COUNT;
            return k < LOADER_DATA ? EntangledDeviceMenu.encode(k, link, online, extra(0), extra(1)) : loaderData(k - LOADER_DATA);
        }

        @Override
        public void set(int id, int value) {
            if (id < EnergyMenu.DATA_COUNT) data.set(id, value);
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected EntangledDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, String kind) {
        this(type, pos, state, size, kind, CAPACITY, MAX_IN, 0);
    }

    protected EntangledDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, String kind, int capacity, int maxIn, int maxOut) {
        super(type, pos, state, size, capacity, maxIn, maxOut);
        this.kind = kind;
    }

    protected int extra(int i) {
        return 0;
    }

    /** The shared channel, or null while there is no code or no power. */
    protected EntangledData.@Nullable Channel channel() {
        if (link == null || !online || !(level instanceof ServerLevel server)) return null;
        return EntangledData.get(server.getServer()).channel(kind, link);
    }

    public boolean online() {
        return online;
    }

    @Override
    public @Nullable EntangledLink link() {
        return link;
    }

    @Override
    public void setLink(@Nullable EntangledLink link) {
        this.link = link;
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (link != null && level instanceof ServerLevel server) EntangledData.get(server.getServer()).setDirty();
    }

    @Override
    protected boolean isOutput(int slot) {
        return false;
    }

    @Override
    public boolean energyOut(@Nullable Direction side) {
        return false;
    }

    /** Public devices work with anything; private ones only with the owner's own pipes and cables. */
    @Override
    public boolean exposed() {
        return EntangledAccess.allows(level, worldPosition, link);
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return exposed();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return exposed() ? super.getSlotsForFace(side) : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return exposed() && super.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return exposed() && super.canTakeItemThroughFace(slot, stack, side);
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final String scope = link == null ? "public" : link.scope();
        if (!scope.equals(sharedScope)) {
            sharedScope = scope;
            Transfer.invalidator.invalidate(level, pos);
            Conduits.refreshAround(level, pos);
        }
        final boolean was = online;
        online = link != null && energy.energy >= upkeep();
        if (online) energy.energy -= upkeep();
        if (was != online) setChanged();
        setLit(level, pos, state, lit());
    }

    public enum LoaderStatus { NONE, OFF, DISABLED, NO_CODE, ACTIVE, AWAY, GRACE, NO_POWER, OVER_LIMIT }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        super.serverTick(level, pos, state);
        loaderTick(level, pos);
        energy.maxIn += draw;
    }

    /** Chunk radius the fitted modules ask for, or -1 without a Chunk Loader. */
    private int wantedRadius(MinecraftServer server) {
        if (!upgrades().contains(Upgrade.CHUNK_LOADER)) return -1;
        final int width = Math.max(1, ServerConfig.integer(server, ServerConfig.MAX_RANGE));
        return Math.min(Math.min(Upgrade.MAX_RANGE, Upgrade.count(upgrades(), Upgrade.RANGE)), (width - 1) / 2);
    }

    private void loaderTick(ServerLevel level, BlockPos pos) {
        final MinecraftServer server = level.getServer();
        final ChunkLoaders loaders = ChunkLoaders.get(server);
        final GlobalPos at = GlobalPos.of(level.dimension(), pos);
        final int r = wantedRadius(server);
        radius = r;
        draw = 0;
        LoaderStatus next;
        if (r < 0) next = LoaderStatus.NONE;
        else if (!ServerConfig.bool(server, ServerConfig.CHUNK_LOADING)) next = LoaderStatus.DISABLED;
        else if (!loaderOn) next = LoaderStatus.OFF;
        else if (link == null) next = LoaderStatus.NO_CODE;
        else {
            draw = ServerConfig.integer(server, ServerConfig.FE_PER_CHUNK) * ChunkLoaders.side(r) * ChunkLoaders.side(r);
            if (powerDown && energy.energy < restartEnergy()) next = LoaderStatus.NO_POWER;
            else {
                if (powerDown) {
                    powerDown = false;
                    setChanged();
                }
                if (!loaders.start(server, at, link.owner(), r)) next = LoaderStatus.OVER_LIMIT;
                else if (!loaders.holding(at)) next = LoaderStatus.AWAY;
                else if (energy.energy >= draw) {
                    energy.energy -= draw;
                    grace = 0;
                    next = LoaderStatus.ACTIVE;
                } else if (++grace < GRACE) next = LoaderStatus.GRACE;
                else {
                    grace = 0;
                    powerDown = true;
                    setChanged();
                    next = LoaderStatus.NO_POWER;
                }
            }
        }
        // switched off server-wide keeps the entry, so turning it back on loads the chunks again
        if (next == LoaderStatus.DISABLED) loaders.resync(server, at);
        else if (next != LoaderStatus.ACTIVE && next != LoaderStatus.GRACE && next != LoaderStatus.AWAY) loaders.stop(server, at);
        if (next != loader) {
            loader = next;
            setChanged();
        }
    }

    private int restartEnergy() {
        return Math.min(energy.capacity, RESTART_TICKS * (draw + upkeep()));
    }

    /** Whether this device means to hold its chunks, so a saved loader entry for it is still good. */
    public boolean loading() {
        return upgrades().contains(Upgrade.CHUNK_LOADER) && loaderOn && link != null && !powerDown;
    }

    public LoaderStatus loaderStatus() {
        return loader;
    }

    public int loaderDraw() {
        return draw;
    }

    public void toggleLoader() {
        if (!upgrades().contains(Upgrade.CHUNK_LOADER)) return;
        loaderOn = !loaderOn;
        grace = 0;
        setChanged();
    }

    protected void dropLoader() {
        if (level instanceof ServerLevel server) ChunkLoaders.get(server.getServer()).stop(server.getServer(), GlobalPos.of(server.dimension(), worldPosition));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropLoader();
        super.preRemoveSideEffects(pos, state);
    }

    /** Status, on switch, radius and grace seconds; draw in two halves; the cap and what the owner uses now. */
    private int loaderData(int k) {
        return switch (k) {
            case 0 -> loader.ordinal() | (radius < 0 ? 0 : radius) << 4 | (loaderOn ? 1 << 6 : 0) | Math.min(15, (GRACE - grace + 19) / 20) << 8;
            case 1 -> draw & 0xFFFF;
            case 2 -> draw >>> 16;
            case 3 -> level instanceof ServerLevel server ? Math.min(0xFFFF, ServerConfig.integer(server.getServer(), ServerConfig.MAX_CHUNKS)) : 0;
            case 4 -> level instanceof ServerLevel server && link != null ? Math.min(0xFFFF, ChunkLoaders.get(server.getServer()).used(link.owner())) : 0;
            default -> 0;
        };
    }

    protected int upkeep() {
        return UPKEEP;
    }

    protected boolean lit() {
        return online;
    }

    @Override
    public boolean canOpen(Player player) {
        if (!EntangledLink.allows(link, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_not_owner").withStyle(ChatFormatting.RED));
            return false;
        }
        return super.canOpen(player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        link = input.read("entangled_link", EntangledLink.CODEC).orElse(null);
        online = input.getBooleanOr("online", false);
        loaderOn = input.getBooleanOr("chunk_loader_on", true);
        powerDown = input.getBooleanOr("chunk_loader_down", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (link != null) output.store("entangled_link", EntangledLink.CODEC, link);
        output.putBoolean("online", online);
        output.putBoolean("chunk_loader_on", loaderOn);
        output.putBoolean("chunk_loader_down", powerDown);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (link != null) components.set(SimpleMachines.ENTANGLED_LINK, link);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        link = components.get(SimpleMachines.ENTANGLED_LINK);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("entangled_link");
    }
}
