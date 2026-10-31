package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.cable.CableBlock;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import com.workshopcraft.simplemachines.cable.Conduits;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import com.workshopcraft.simplemachines.pipe.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Moves FE between relays on one code, in any dimension. Senders take power from cables and hand it to the receivers
 * round robin, losing a tenth on the way; receivers push it out at their own voltage.
 */
public class EntangledRelayBlockEntity extends EntangledDeviceBlockEntity {
    public static final int[] CAPACITY = {20_000, 160_000, 1_280_000};
    public static final int LOSS_PERCENT = 10;
    public static final int DATA_COUNT = EntangledDeviceBlockEntity.DATA_COUNT + 1;
    public final int voltage;
    private boolean receive;
    private int lastEnd, flowIn, flowOut, peers, turn;
    private long lastTick = -1, gotTick = -1;
    private int got;
    private @Nullable String joined;

    private final ContainerData relayData = new ContainerData() {
        @Override
        public int get(int id) {
            if (id < EntangledDeviceBlockEntity.DATA_COUNT) return menuData.get(id);
            return Math.min(flowOut, 0xFFFF);
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EntangledRelayBlockEntity(BlockPos pos, BlockState state, int voltage) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.RELAYS[voltage]), pos, state, 0, EntangledData.RELAY,
                CAPACITY[voltage], Voltage.RATE[voltage], Voltage.RATE[voltage]);
        this.voltage = voltage;
    }

    public boolean receiving() {
        return receive;
    }

    public void toggleMode() {
        receive = !receive;
        setChanged();
        if (level instanceof ServerLevel server) {
            Transfer.invalidator.invalidate(server, worldPosition);
            Conduits.refreshAround(server, worldPosition);
        }
    }

    @Override
    protected int extra(int i) {
        return i == 0 ? (receive ? 1 : 0) | Math.min(peers, 0x7FFF) << 1 : Math.min(flowIn, 0xFFFF);
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return !receive && exposed();
    }

    @Override
    public boolean energyOut(@Nullable Direction side) {
        return receive && exposed();
    }

    @Override
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        super.serverTick(level, pos, state);
        energy.maxIn = receive ? 0 : Voltage.RATE[voltage];
        energy.maxOut = Voltage.RATE[voltage];
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final long now = level.getGameTime();
        if (now == lastTick) return;
        lastTick = now;
        flowIn = Math.max(0, energy.energy - lastEnd);
        super.work(level, pos, state, tier);
        final Set<EntangledRelayBlockEntity> on = join(EntangledData.get(level.getServer()));
        final List<EntangledRelayBlockEntity> others = new ArrayList<>();
        if (on != null) for (EntangledRelayBlockEntity r : on) if (r != this && !r.isRemoved() && r.receive != receive) others.add(r);
        peers = others.size();
        flowOut = 0;
        if (online && !receive) send(others, now);
        if (online && receive) push(level, pos);
        lastEnd = energy.energy;
    }

    private @Nullable Set<EntangledRelayBlockEntity> join(EntangledData data) {
        final String key = link == null ? null : EntangledData.key(EntangledData.RELAY, link);
        if (joined != null && !joined.equals(key)) leave(data);
        joined = key;
        if (key == null) return null;
        final Set<EntangledRelayBlockEntity> set = data.relays.computeIfAbsent(key, k -> new LinkedHashSet<>());
        set.add(this);
        return set;
    }

    private void leave(EntangledData data) {
        final Set<EntangledRelayBlockEntity> set = data.relays.get(joined);
        if (set != null) {
            set.remove(this);
            if (set.isEmpty()) data.relays.remove(joined);
        }
        joined = null;
    }

    @Override
    public void setRemoved() {
        if (joined != null && level instanceof ServerLevel server) leave(EntangledData.get(server.getServer()));
        super.setRemoved();
    }

    /** Splits this tick's budget evenly over the receivers, starting one further along each tick. */
    private void send(List<EntangledRelayBlockEntity> to, long now) {
        if (to.isEmpty()) return;
        int budget = Math.min(energy.energy, Voltage.RATE[voltage]);
        final int n = to.size();
        turn = (turn + 1) % n;
        for (int i = 0; i < n && budget > 0; i++) {
            final EntangledRelayBlockEntity r = to.get((turn + i) % n);
            final int share = budget / (n - i);
            final int delivered = Math.min(share * (100 - LOSS_PERCENT) / 100, r.room(now));
            if (delivered <= 0) continue;
            final int cost = Math.min(budget, (delivered * 100 + 100 - LOSS_PERCENT - 1) / (100 - LOSS_PERCENT));
            r.take(delivered, now);
            energy.energy -= cost;
            budget -= cost;
            flowOut += cost;
        }
        if (flowOut > 0) setChanged();
    }

    private int room(long now) {
        if (gotTick != now) {
            gotTick = now;
            got = 0;
        }
        return Math.max(0, Math.min(energy.capacity - energy.energy, Voltage.RATE[voltage] - got));
    }

    private void take(int n, long now) {
        room(now);
        got += n;
        energy.energy += n;
        setChanged();
    }

    /** Like a generator: a lower rated block of ours on a side blows. Private relays only feed their owner's cables. */
    private void push(ServerLevel level, BlockPos pos) {
        int left = Math.min(energy.energy, Voltage.RATE[voltage]);
        final Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length && left > 0; i++) {
            final Direction d = dirs[(turn + i) % dirs.length];
            final BlockPos target = pos.relative(d);
            if (link != null && link.priv() && !(level.getBlockState(target).getBlock() instanceof CableBlock
                    && link.owner().equals(ConduitBlockEntity.ownerAt(level, target)))) continue;
            if (Voltage.overloads(level, target, d.getOpposite(), voltage)) {
                if (EnergyBuffer.lookup.has(level, target, d.getOpposite())) {
                    Voltage.blow(level, target);
                    return;
                }
                continue;
            }
            final int n = EnergyBuffer.pusher.push(level, target, d.getOpposite(), left);
            if (n > 0) {
                left -= n;
                energy.energy -= n;
                flowOut += n;
            }
        }
        turn = (turn + 1) % dirs.length;
        if (flowOut > 0) setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        receive = input.getBooleanOr("receive", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("receive", receive);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.simplemachines." + Core.RELAYS[voltage].block);
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new EntangledRelayMenu(id, this, relayData, this);
    }
}
