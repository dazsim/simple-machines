package com.workshopcraft.simplemachines.quarry;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.assembly.Tier;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import com.workshopcraft.simplemachines.machine.AreaBlockEntity;
import com.workshopcraft.simplemachines.machine.MachineBlock;
import com.workshopcraft.simplemachines.pipe.PipeFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
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

/**
 * MV quarry, 8000 FE a block. Sits at one corner of its area, builds a frame round it, runs a gantry and drill head above it
 * and digs the inside layer by layer down to bedrock. Landmarks mark the far corners, otherwise 16x16.
 */
public class QuarryBlockEntity extends AreaBlockEntity {
    public static final int DEFAULT = 16;
    public static final int MAX = 64;
    private static final int SCAN = 1024;
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
        if (done || !hasRoom()) {
            setLit(level, pos, state, false);
            return;
        }
        acc = Math.min(acc + drill().speed(tier, upgrades()) / 20F, 4F);
        total = 100;
        final QuarryDrill d = drill();
        for (int scans = 0; scans < SCAN && !done; scans++) {
            final BlockPos p = cell(pos, facing);
            if (!level.isLoaded(p)) break;
            final BlockState s = level.getBlockState(p);
            if (diggable(level, p, s, d)) {
                if (acc < 1F || !hasRoom() || energy.energy < PER_BLOCK) break;
                dig(level, p, s, d);
                energy.energy -= PER_BLOCK;
                acc -= 1F;
            }
            advance(level, pos, facing);
        }
        progress = Math.round(Math.min(1F, acc) * 100);
        if (!done) moveHead(level, pos, facing);
        setLit(level, pos, level.getBlockState(pos), !done && energy.energy >= PER_BLOCK);
        setChanged();
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
        for (BlockPos p : ring(pos, facing)) {
            if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, SimpleMachines.quarryFrame.defaultBlockState(), 3);
        }
    }

    private static int find(Level level, BlockPos pos, Direction d) {
        for (int k = 2; k <= MAX + 1; k++) if (level.getBlockState(pos.relative(d, k)).is(SimpleMachines.landmark)) return k;
        return 0;
    }

    private BlockPos at(BlockPos pos, Direction facing, int a, int b, int y) {
        final BlockPos p = pos.relative(facing, a).relative(side, b);
        return new BlockPos(p.getX(), y, p.getZ());
    }

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
        final int r = cursor / span();
        int c = cursor % span();
        if (r % 2 == 1) c = span() - 1 - c;
        return at(pos, facing, c + 1, r + 1, layer);
    }

    private void advance(ServerLevel level, BlockPos pos, Direction facing) {
        if (++cursor < span() * (wid - 1)) return;
        cursor = 0;
        if (--layer < level.getMinY()) {
            done = true;
            clearGantry(level, pos, facing);
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

    private void moveHead(ServerLevel level, BlockPos pos, Direction facing) {
        final int r = cursor / span();
        final int y = pos.getY() + 1;
        if (r != row) {
            clearGantry(level, pos, facing);
            row = r;
            for (int a = 0; a <= len; a++) {
                final BlockPos p = at(pos, facing, a, r + 1, y);
                if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, SimpleMachines.quarryFrame.defaultBlockState(), 3);
            }
        }
        final BlockPos c = cell(pos, facing);
        final BlockPos h = new BlockPos(c.getX(), y, c.getZ());
        if (h.equals(head)) return;
        if (head != null && level.getBlockState(head).is(SimpleMachines.quarryHead)) {
            level.setBlock(head, SimpleMachines.quarryFrame.defaultBlockState(), 3);
        }
        final BlockState at = level.getBlockState(h);
        if (at.canBeReplaced() || at.is(SimpleMachines.quarryFrame)) {
            level.setBlock(h, SimpleMachines.quarryHead.defaultBlockState(), 3);
            head = h;
        } else {
            head = null;
        }
    }

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
        if (level == null || level.isClientSide()) return;
        if (!filter.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filter);
            filter = ItemStack.EMPTY;
        }
        if (!planned) return;
        final Direction facing = state.getValue(MachineBlock.FACING);
        clearGantry(level, pos, facing);
        for (BlockPos p : ring(pos, facing)) if (level.getBlockState(p).is(SimpleMachines.quarryFrame)) level.removeBlock(p, false);
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
