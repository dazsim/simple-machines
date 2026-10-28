package com.workshopcraft.simplemachines.quarry;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.item.ToolSparks;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import com.workshopcraft.simplemachines.machine.AreaBlockEntity;
import com.workshopcraft.simplemachines.machine.MachineBlock;
import com.workshopcraft.simplemachines.pipe.PipeFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MV quarry, 8000 FE a block. Sits at one corner of its area under a raised frame, runs a gantry and drill head along it
 * and digs the inside layer by layer down to bedrock. Landmarks mark the far corners, otherwise 16x16. Stops while any of
 * the frame is missing and puts it back a piece at a time.
 */
public class QuarryBlockEntity extends AreaBlockEntity {
    public static final int DEFAULT = 16;
    public static final int MAX = 64;
    private static final int SCAN = 1024;
    private static final int RESCAN = 512;
    private static final int REPAIR = 10;
    private static final int BUILD = 3;
    private static final float DEPTH = 0.75F;
    public static final int PER_BLOCK = 8000;
    public static final int BUFFER = 200_000;

    private String drill = "";
    private ItemStack filter = ItemStack.EMPTY;
    private boolean planned;
    private int len;
    private int wid;
    private Direction side = Direction.EAST;
    private int layer;
    private int cursor;
    private int row = -1;
    private @Nullable BlockPos head;
    private boolean done;
    private float acc;
    private long lastTick = -1;
    private long dug;
    private @Nullable BlockPos extra;
    private int rescan;
    private @Nullable List<Piece> frame;
    private @Nullable BlockPos cracked;
    private int crackStage = -1;
    private @Nullable BlockPos target;
    private boolean paused;
    private boolean broken;
    private boolean built;
    private float rate;
    private float sentRate = -1;
    // client view
    private int serial;
    private int seen = -1;
    private @Nullable BlockPos viewTo;
    private final double[] from = new double[3];
    private final double[] to = new double[3];
    private float moveStart;
    private float moveTicks = 1;
    private float accStart;
    private float accAt;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(Core.QUARRY), pos, state, BUFFER, Voltage.RATE[Voltage.MV]);
    }

    public QuarryDrill drill() {
        return QuarryDrill.parse(drill);
    }

    public String drillData() {
        return drill;
    }

    public void setDrill(String data) {
        drill = data == null ? "" : data;
        setChanged();
    }

    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.copy();
        setChanged();
    }

    @Override
    public boolean energyIn(@Nullable Direction side) {
        return side != null && level != null && Voltage.sending(level, getBlockPos().relative(side), side.getOpposite()) == Voltage.MV;
    }

    @Override
    protected int defaultOutputs(BlockState state) {
        return 63;
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        final long now = level.getGameTime();
        if (now == lastTick) return;
        lastTick = now;
        final Direction facing = state.getValue(MachineBlock.FACING);
        if (!planned) plan(level, pos, facing);
        if (row >= 0) clearGantry(level, pos, facing);
        total = 100;
        final Piece gap = gap(level, pos, facing);
        if (gap != null) {
            // no digging until the whole frame is up, built a piece at a time once there is power
            if (energy.energy > 0 && now % (built ? REPAIR : BUILD) == 0) {
                level.setBlock(gap.pos, gap.state, 3);
                level.playSound(null, gap.pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            crack(level, null);
            sync(level, target, true, true);
            setLit(level, pos, state, false);
            setChanged();
            return;
        }
        if (!built) {
            built = true;
            setChanged();
        }
        final QuarryDrill d = drill();
        if (extra == null) rescan(level, pos, facing, d);
        final float rate = d.speed(tier, upgrades()) / 20F;
        boolean ready = hasRoom() && energy.energy >= PER_BLOCK;
        if (ready) acc = Math.min(acc + rate, 1F + rate);
        BlockPos aim = null;
        boolean solid = false;
        if (extra != null) {
            final BlockState s = level.isLoaded(extra) ? level.getBlockState(extra) : null;
            if (s == null || !diggable(level, extra, s, d)) {
                extra = null;
            } else if (ready && acc >= 1F) {
                dig(level, extra, s, d);
                energy.energy -= PER_BLOCK;
                acc -= 1F;
                extra = null;
                ready = hasRoom() && energy.energy >= PER_BLOCK;
            } else {
                aim = extra;
                solid = true;
            }
        }
        if (aim == null && !done) {
            for (int scans = 0; scans < SCAN && !done; scans++) {
                final BlockPos p = cell(pos, facing);
                if (!level.isLoaded(p)) break;
                final BlockState s = level.getBlockState(p);
                if (diggable(level, p, s, d)) {
                    if (acc < 1F || !ready) {
                        aim = p;
                        solid = true;
                        break;
                    }
                    dig(level, p, s, d);
                    energy.energy -= PER_BLOCK;
                    acc -= 1F;
                    ready = hasRoom() && energy.energy >= PER_BLOCK;
                }
                advance(level, pos, facing);
            }
            if (aim == null && !done) aim = cell(pos, facing);
        }
        progress = Math.round(Math.min(1F, acc) * 100);
        crack(level, solid ? aim : null);
        if (solid && ready && now % 2 == 0) ToolSparks.quarry(level, aim, level.getBlockState(aim), Math.min(1F, acc) * DEPTH);
        this.rate = rate;
        sync(level, aim, !ready, false);
        setLit(level, pos, level.getBlockState(pos), aim != null && energy.energy >= PER_BLOCK);
        setChanged();
    }

    private void sync(ServerLevel level, @Nullable BlockPos aim, boolean stopped, boolean broken) {
        if (Objects.equals(aim, target) && stopped == paused && broken == this.broken && rate == sentRate) return;
        target = aim;
        paused = stopped;
        this.broken = broken;
        sentRate = rate;
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    private void crack(ServerLevel level, @Nullable BlockPos at) {
        final int stage = at == null ? -1 : Math.min(9, (int) (acc * 10F));
        if (Objects.equals(at, cracked) && stage == crackStage) return;
        final int id = breaker();
        if (cracked != null && !cracked.equals(at)) level.destroyBlockProgress(id, cracked, -1);
        if (at != null) level.destroyBlockProgress(id, at, stage);
        cracked = at;
        crackStage = stage;
    }

    private int breaker() {
        return -1000 - (getBlockPos().hashCode() & 0xFFFFFF);
    }

    private void plan(ServerLevel level, BlockPos pos, Direction facing) {
        final int l = find(level, pos, facing);
        int w = find(level, pos, facing.getClockWise());
        side = facing.getClockWise();
        if (w == 0) {
            w = find(level, pos, facing.getCounterClockWise());
            if (w > 0) side = facing.getCounterClockWise();
        }
        len = l == 0 ? DEFAULT + 1 : l;
        wid = w == 0 ? DEFAULT + 1 : w;
        layer = pos.getY();
        cursor = 0;
        done = false;
        planned = true;
        frame = null;
        built = false;
    }

    private static int find(Level level, BlockPos pos, Direction d) {
        for (int k = 2; k <= MAX + 1; k++) if (level.getBlockState(pos.relative(d, k)).is(SimpleMachines.landmark)) return k;
        return 0;
    }

    private BlockPos at(BlockPos pos, Direction facing, int a, int b, int y) {
        final BlockPos p = pos.relative(facing, a).relative(side, b);
        return new BlockPos(p.getX(), y, p.getZ());
    }

    record Piece(BlockPos pos, BlockState state) {
    }

    /** Corner posts two blocks high with rails along the top edges. */
    private List<Piece> frame(BlockPos pos, Direction facing) {
        if (frame != null) return frame;
        if (len < 2 || wid < 2) return List.of();
        final List<Piece> out = new ArrayList<>();
        final BlockState post = SimpleMachines.quarryFrame.defaultBlockState();
        final BlockState rail = post.setValue(QuarryFrameBlock.POST, false);
        final int y = pos.getY();
        for (int a : new int[]{0, len}) {
            for (int b : new int[]{0, wid}) {
                if (a != 0 || b != 0) out.add(new Piece(at(pos, facing, a, b, y), post));
                out.add(new Piece(at(pos, facing, a, b, y + 1), post));
                final Direction da = a == 0 ? facing : facing.getOpposite();
                final Direction db = b == 0 ? side : side.getOpposite();
                out.add(new Piece(at(pos, facing, a, b, y + 2),
                        post.setValue(QuarryFrameBlock.arm(da), true).setValue(QuarryFrameBlock.arm(db), true)));
            }
        }
        final BlockState alongA = rail.setValue(QuarryFrameBlock.arm(facing), true).setValue(QuarryFrameBlock.arm(facing.getOpposite()), true);
        final BlockState alongB = rail.setValue(QuarryFrameBlock.arm(side), true).setValue(QuarryFrameBlock.arm(side.getOpposite()), true);
        for (int a = 1; a < len; a++) {
            out.add(new Piece(at(pos, facing, a, 0, y + 2), alongA));
            out.add(new Piece(at(pos, facing, a, wid, y + 2), alongA));
        }
        for (int b = 1; b < wid; b++) {
            out.add(new Piece(at(pos, facing, 0, b, y + 2), alongB));
            out.add(new Piece(at(pos, facing, len, b, y + 2), alongB));
        }
        frame = List.copyOf(out);
        return frame;
    }

    /** The first frame piece that has gone and can be put back, if any. */
    private @Nullable Piece gap(ServerLevel level, BlockPos pos, Direction facing) {
        for (Piece piece : frame(pos, facing)) {
            if (!level.isLoaded(piece.pos)) continue;
            final BlockState s = level.getBlockState(piece.pos);
            if (!s.is(SimpleMachines.quarryFrame) && s.canBeReplaced()) return piece;
        }
        return null;
    }

    /** True while a frame piece is missing and the quarry is waiting for the rebuild. */
    public boolean broken() {
        return broken;
    }

    // the old ground ring from 2.1.0-alpha.10 and earlier
    private List<BlockPos> ring(BlockPos pos, Direction facing) {
        final List<BlockPos> out = new ArrayList<>();
        for (int a = 0; a <= len; a++) {
            for (int b = 0; b <= wid; b++) {
                if (a != 0 && a != len && b != 0 && b != wid) continue;
                if (a == 0 && b == 0) continue;
                out.add(at(pos, facing, a, b, pos.getY()));
            }
        }
        return out;
    }

    private int span() {
        return len - 1;
    }

    private BlockPos cell(BlockPos pos, Direction facing) {
        return cellAt(pos, facing, cursor, layer);
    }

    private BlockPos cellAt(BlockPos pos, Direction facing, int i, int y) {
        final int r = i / span();
        int c = i % span();
        if (r % 2 == 1) c = span() - 1 - c;
        return at(pos, facing, c + 1, r + 1, y);
    }

    private void advance(ServerLevel level, BlockPos pos, Direction facing) {
        if (++cursor < span() * (wid - 1)) return;
        cursor = 0;
        if (--layer < level.getMinY()) done = true;
    }

    /** Looks back over the layers already dug for anything put there since, a few hundred cells a tick. */
    private void rescan(ServerLevel level, BlockPos pos, Direction facing, QuarryDrill d) {
        if (len < 2 || wid < 2) return;
        final int area = span() * (wid - 1);
        final int top = pos.getY() + 2;
        final int bottom = done ? level.getMinY() : layer;
        final int cells = (top - bottom + 1) * area;
        for (int n = 0; n < RESCAN && extra == null; n++) {
            if (rescan >= cells || rescan < 0) rescan = 0;
            final int y = top - rescan / area;
            final int i = rescan % area;
            rescan++;
            if (!done && y == layer && i >= cursor) continue;
            final BlockPos p = cellAt(pos, facing, i, y);
            if (!level.isLoaded(p)) continue;
            if (diggable(level, p, level.getBlockState(p), d)) extra = p;
        }
    }

    private boolean diggable(ServerLevel level, BlockPos p, BlockState s, QuarryDrill d) {
        if (s.isAir() || !s.getFluidState().isEmpty()) return false;
        if (s.getDestroySpeed(level, p) < 0) return false;
        if (s.is(SimpleMachines.quarryFrame) || s.is(SimpleMachines.quarryHead) || s.is(SimpleMachines.landmark)) return false;
        if (s.getBlock() instanceof QuarryBlock) return false;
        if (PoweredToolItem.hard(s) && !d.diamond()) return false;
        return !s.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
    }

    private void dig(ServerLevel level, BlockPos p, BlockState s, QuarryDrill d) {
        final BlockEntity be = level.getBlockEntity(p);
        final ItemStack tool = new ItemStack(d.diamond() ? Items.DIAMOND_PICKAXE : Items.IRON_PICKAXE);
        final List<ItemStack> drops = new ArrayList<>(Block.getDrops(s, level, p, be, null, tool));
        if (be instanceof Container c) {
            for (int i = 0; i < c.getContainerSize(); i++) if (!c.getItem(i).isEmpty()) drops.add(c.getItem(i).copy());
            c.clearContent();
        }
        level.levelEvent(2001, p, Block.getId(s));
        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        dug++;
        final PipeFilter pf = filter.isEmpty() ? null : filter.getOrDefault(SimpleMachines.FILTER, PipeFilter.EMPTY);
        for (ItemStack drop : drops) {
            if (drop.isEmpty() || pf != null && !pf.accepts(drop)) continue;
            keep(level, drop);
        }
    }

    // gantry and head blocks left by 2.1.0-alpha.10 and earlier
    private void clearGantry(Level level, BlockPos pos, Direction facing) {
        if (head != null && level.getBlockState(head).is(SimpleMachines.quarryHead)) level.removeBlock(head, false);
        head = null;
        if (row < 0) return;
        for (int a = 0; a <= len; a++) {
            final BlockPos p = at(pos, facing, a, row + 1, pos.getY() + 1);
            if (level.getBlockState(p).is(SimpleMachines.quarryFrame) || level.getBlockState(p).is(SimpleMachines.quarryHead)) {
                level.removeBlock(p, false);
            }
        }
        row = -1;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel server)) return;
        if (!filter.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter);
            filter = ItemStack.EMPTY;
        }
        if (cracked != null) server.destroyBlockProgress(breaker(), cracked, -1);
        cracked = null;
        if (!planned) return;
        final Direction facing = state.getValue(MachineBlock.FACING);
        clearGantry(level, pos, facing);
        for (Piece piece : frame(pos, facing)) if (level.getBlockState(piece.pos).is(SimpleMachines.quarryFrame)) level.removeBlock(piece.pos, false);
        for (BlockPos p : ring(pos, facing)) if (level.getBlockState(p).is(SimpleMachines.quarryFrame)) level.removeBlock(p, false);
    }

    // what the client draws: gantry over the column being dug, drill lowering into it

    /** World position of the drill tip at the given client time in ticks, or null when there is nothing to show. */
    public double @Nullable [] tip(float time) {
        if (target == null || !planned || !built) {
            viewTo = null;
            seen = serial;
            return null;
        }
        if (seen != serial) {
            seen = serial;
            if (!target.equals(viewTo)) {
                final double[] now = viewTo == null ? null : tipAt(time);
                viewTo = target;
                to[0] = target.getX() + 0.5;
                to[1] = target.getY() + 1;
                to[2] = target.getZ() + 0.5;
                if (now == null) System.arraycopy(to, 0, from, 0, 3);
                else System.arraycopy(now, 0, from, 0, 3);
                final double dist = Math.sqrt(sq(to[0] - from[0]) + sq(to[1] - from[1]) + sq(to[2] - from[2]));
                moveStart = time;
                moveTicks = (float) Math.max(2, Math.min(30, dist / 0.35));
            }
            accStart = time;
            accAt = acc;
        }
        return tipAt(time);
    }

    private double[] tipAt(float time) {
        final float m = smooth(Math.min(1F, Math.max(0F, (time - moveStart) / moveTicks)));
        final float p = Math.min(1F, Math.max(0F, accAt + (paused ? 0F : rate) * (time - accStart)));
        return new double[]{from[0] + (to[0] - from[0]) * m, from[1] + (to[1] - from[1]) * m - p * m * DEPTH,
                from[2] + (to[2] - from[2]) * m};
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }

    private static double sq(double v) {
        return v * v;
    }

    public int length() {
        return len;
    }

    public int width() {
        return wid;
    }

    public Direction side() {
        return side;
    }

    public boolean planned() {
        return planned;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("planned", planned);
        tag.putInt("length", len);
        tag.putInt("width", wid);
        tag.putString("side", side.getSerializedName());
        if (target != null) tag.putLong("target", target.asLong());
        tag.putFloat("acc", acc);
        tag.putFloat("rate", rate);
        tag.putBoolean("paused", paused);
        tag.putBoolean("broken", broken);
        tag.putBoolean("built", built);
        return tag;
    }

    /** Status line for a right click with a screwdriver or the tests. */
    public Component status() {
        return Component.translatable("message.simplemachines.quarry", wid - 1, len - 1, layer, dug);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        drill = input.getStringOr("drill", "");
        filter = input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        planned = input.getBooleanOr("planned", false);
        len = input.getIntOr("length", 0);
        wid = input.getIntOr("width", 0);
        side = Direction.byName(input.getStringOr("side", "east"));
        if (side == null) side = Direction.EAST;
        layer = input.getIntOr("layer", 0);
        cursor = input.getIntOr("cursor", 0);
        row = input.getIntOr("row", -1);
        head = input.getLong("head").map(BlockPos::of).orElse(null);
        done = input.getBooleanOr("done", false);
        acc = input.getFloatOr("acc", 0F);
        dug = input.getLongOr("dug", 0L);
        rescan = input.getIntOr("rescan", 0);
        extra = input.getLong("extra").map(BlockPos::of).orElse(null);
        frame = null;
        target = input.getLong("target").map(BlockPos::of).orElse(null);
        rate = input.getFloatOr("rate", 0F);
        paused = input.getBooleanOr("paused", false);
        broken = input.getBooleanOr("broken", false);
        built = input.getBooleanOr("built", false);
        serial++;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("drill", drill);
        if (!filter.isEmpty()) output.store("filter", ItemStack.CODEC, filter);
        output.putBoolean("planned", planned);
        output.putInt("length", len);
        output.putInt("width", wid);
        output.putString("side", side.getSerializedName());
        output.putInt("layer", layer);
        output.putInt("cursor", cursor);
        output.putInt("row", row);
        if (head != null) output.putLong("head", head.asLong());
        output.putBoolean("done", done);
        output.putFloat("acc", acc);
        output.putLong("dug", dug);
        output.putInt("rescan", rescan);
        output.putBoolean("built", built);
        if (extra != null) output.putLong("extra", extra.asLong());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!drill.isEmpty()) components.set(SimpleMachines.QUARRY_DRILL, drill);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        drill = components.getOrDefault(SimpleMachines.QUARRY_DRILL, "");
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("drill");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.simplemachines.quarry");
    }
}
