package com.workshopcraft.simplemachines.cable;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Two one-way buffers: up holds energy from the low faces bound for the front, down holds energy from the front bound for the low faces. 5% is lost on the way out. */
public class TransformerBlockEntity extends BlockEntity {
    public final EnergyBuffer up;
    public final EnergyBuffer down;
    public final int high;
    private int nextSide;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.transformerEntity, pos, state);
        high = state.getBlock() instanceof TransformerBlock t ? t.high : Voltage.MV;
        final int rate = rate(high);
        up = new EnergyBuffer(rate * 10, rate, rate, this::setChanged);
        down = new EnergyBuffer(rate * 10, rate, rate, this::setChanged);
    }

    /** FE/t a transformer with this high side moves each way; each buffer holds ten ticks of it. */
    public static int rate(int high) {
        return Voltage.RATE[high - 1];
    }

    /** The buffer energy offered on side goes into. */
    public EnergyBuffer input(@Nullable Direction side) {
        return side != null && side == getBlockState().getValue(TransformerBlock.FACING) ? down : up;
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        final Direction front = state.getValue(TransformerBlock.FACING);
        send(level, pos, front, up, high, up.maxOut);
        int left = down.maxOut;
        final Direction[] dirs = Direction.values();
        final int start = nextSide;
        for (int i = 0; i < dirs.length && left > 0; i++) {
            final Direction d = dirs[(start + i) % dirs.length];
            if (d == front) continue;
            final int n = send(level, pos, d, down, high - 1, left);
            if (n > 0) nextSide = (start + i + 1) % dirs.length;
            left -= n;
        }
    }

    private int send(ServerLevel level, BlockPos pos, Direction d, EnergyBuffer from, int tier, int limit) {
        final int avail = Math.min(limit, (int) (from.energy * 19L / 20));
        if (avail <= 0) return 0;
        final BlockPos target = pos.relative(d);
        if (Voltage.overloads(level, target, d.getOpposite(), tier)) {
            if (EnergyBuffer.lookup.has(level, target, d.getOpposite())) Voltage.blow(level, target);
            return 0;
        }
        final int n = EnergyBuffer.pusher.push(level, target, d.getOpposite(), avail);
        if (n <= 0) return 0;
        from.energy = Math.max(0, from.energy - n - (n + 18) / 19);
        setChanged();
        return n;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        up.energy = Math.min(up.capacity, input.getIntOr("up", 0));
        down.energy = Math.min(down.capacity, input.getIntOr("down", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("up", up.energy);
        output.putInt("down", down.energy);
    }

    // the item keeps one total; placed again it fills up first, then down
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (up.energy + down.energy > 0) components.set(SimpleMachines.ENERGY, up.energy + down.energy);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        final int e = components.getOrDefault(SimpleMachines.ENERGY, 0);
        up.energy = Math.min(up.capacity, e);
        down.energy = Math.min(down.capacity, e - up.energy);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("up");
        output.discard("down");
    }
}
