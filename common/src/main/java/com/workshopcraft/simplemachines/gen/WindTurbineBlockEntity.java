package com.workshopcraft.simplemachines.gen;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.EnergyBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Lives in the base. Output = 50 per blade x blades x height x openness x weather, measured at the top blade. */
public class WindTurbineBlockEntity extends BlockEntity {
    public static final int MAX = 50;
    public final EnergyBuffer energy = new EnergyBuffer(50000, 0, 500, this::setChanged);
    public float openness = -1;
    private int blades = -1;
    private float part;
    private int age;
    private float rate;
    private float sentRate = -1;
    private int sentBlades = -1;
    // client side: helix angle in degrees and its current turn speed
    public float angle;
    public float speed;
    public long lastFrame = -1;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.windEntity, pos, state);
    }

    /** Height factor for a blade at this position. */
    public static float height(BlockPos top) {
        return Mth.clamp((top.getY() - 64) / 96F, 0F, 1F);
    }

    public static float weather(Level level) {
        return level.isThundering() ? 1.5F : level.isRaining() ? 1.25F : 1F;
    }

    /** Working blades stacked on the base, 0 to 5. */
    public static int count(Level level, BlockPos base) {
        int n = 0;
        while (n < TurbineBladeBlock.MAX) {
            final BlockState s = level.getBlockState(base.above(n + 1));
            if (!s.is(SimpleMachines.turbineBlade) || s.getValue(TurbineBladeBlock.STACK) != n + 1) break;
            n++;
        }
        return n;
    }

    /** Blades on top of the fifth that do nothing. */
    public static int spare(Level level, BlockPos base, int blades) {
        int n = 0;
        while (blades == TurbineBladeBlock.MAX && n < 64 && level.getBlockState(base.above(blades + n + 1)).is(SimpleMachines.turbineBlade)) n++;
        return n;
    }

    /** Air share of the 9x9x9 cube around the top blade, not counting the turbine column. */
    public static float measure(Level level, BlockPos base, int blades) {
        final BlockPos top = base.above(Math.max(1, blades));
        int air = 0, all = 0;
        final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    p.set(top.getX() + dx, top.getY() + dy, top.getZ() + dz);
                    // the stack itself, and spare blades above it, are not in the way
                    if (dx == 0 && dz == 0 && (p.getY() >= base.getY() && p.getY() <= top.getY() || level.getBlockState(p).is(SimpleMachines.turbineBlade))) continue;
                    all++;
                    if (level.getBlockState(p).isAir()) air++;
                }
            }
        }
        return air / (float) all;
    }

    public int blades() {
        return Math.max(0, blades);
    }

    /** FE per tick made now. */
    public float making(Level level, BlockPos pos) {
        if (blades < 0) blades = count(level, pos);
        if (blades == 0) return 0;
        if (openness < 0) openness = measure(level, pos, blades);
        return MAX * blades * height(pos.above(blades)) * openness * weather(level);
    }

    /** Server rate synced to the client, FE per tick. */
    public float rate() {
        return rate;
    }

    /** Turn speed in degrees per tick for the client: follows the output of one blade, still at zero. */
    public float targetSpeed() {
        if (blades <= 0 || rate <= 0) return 0;
        return 4F + 14F * Math.min(1.5F, rate / blades / MAX);
    }

    public Component status(Level level, BlockPos pos) {
        blades = count(level, pos);
        openness = measure(level, pos, blades);
        final int extra = spare(level, pos, blades);
        final Component main = Component.translatable("message.simplemachines.wind", String.format("%.1f", making(level, pos)),
                blades, TurbineBladeBlock.MAX, Math.round(height(pos.above(Math.max(1, blades))) * 100), Math.round(openness * 100),
                energy.energy, energy.capacity);
        if (blades == 0) return Component.translatable("message.simplemachines.wind_no_blades", energy.energy, energy.capacity);
        if (extra == 0) return main;
        return main.copy().append(" ").append(Component.translatable("message.simplemachines.wind_spare", extra, TurbineBladeBlock.MAX));
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(WindTurbineBlock.HALF) != DoubleBlockHalf.LOWER) return;
        migrate(level, pos);
        if (age % 20 == 0) {
            final int n = count(level, pos);
            if (n != blades) {
                blades = n;
                openness = -1;
            }
        }
        if (age++ % 100 == 0) openness = measure(level, pos, count(level, pos));
        rate = making(level, pos);
        part += rate;
        final int n = (int) part;
        part -= n;
        final int add = Math.min(n, energy.capacity - energy.energy);
        if (add > 0) {
            energy.energy += add;
            setChanged();
        }
        if (Math.abs(rate - sentRate) > 0.25F || blades != sentBlades) {
            sentRate = rate;
            sentBlades = blades;
            level.sendBlockUpdated(pos, state, state, 3);
        }
        Generators.pushOut(level, pos, energy, Voltage.LV, Direction.UP);
    }

    // 2.1.0-alpha.13 and earlier: the old upper half on top of this base becomes a blade
    private void migrate(ServerLevel level, BlockPos pos) {
        final BlockState up = level.getBlockState(pos.above());
        if (up.is(SimpleMachines.windTurbine) && up.getValue(WindTurbineBlock.HALF) == DoubleBlockHalf.UPPER) {
            level.setBlock(pos.above(), SimpleMachines.turbineBlade.defaultBlockState().setValue(TurbineBladeBlock.STACK, 1), 3);
            blades = -1;
            openness = -1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putFloat("rate", rate);
        tag.putInt("blades", Math.max(0, blades));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.energy = Math.min(energy.capacity, input.getIntOr("energy", 0));
        rate = input.getFloatOr("rate", 0F);
        if (level != null && level.isClientSide()) blades = input.getIntOr("blades", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy.energy);
        output.putFloat("rate", rate);
        output.putInt("blades", Math.max(0, blades));
    }
}
