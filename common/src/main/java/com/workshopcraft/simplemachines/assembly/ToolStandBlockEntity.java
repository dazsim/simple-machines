package com.workshopcraft.simplemachines.assembly;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.item.PoweredToolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A tool being built or kept on a Tool Stand. Steps: 0 empty, 1 frame loose, 2 frame fixed, 3 core loose, 4 core fixed,
 * 5 control board loose, 6 board fixed, 7 battery loose, 8 battery fixed (upgrades fit here). At 8 with nothing loose the tool is done.
 */
public class ToolStandBlockEntity extends BlockEntity {
    public static final int DONE = 8;
    private int step;
    private String frame = "";
    private @Nullable Item tool;
    private int energy;
    private List<Upgrade> upgrades = new ArrayList<>();
    private @Nullable Upgrade loose;

    public ToolStandBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.toolStandEntity, pos, state);
    }

    public int step() {
        return step;
    }

    public String frame() {
        return frame;
    }

    public @Nullable Item tool() {
        return tool;
    }

    public int energy() {
        return energy;
    }

    public List<Upgrade> upgrades() {
        return upgrades;
    }

    public @Nullable Upgrade loose() {
        return loose;
    }

    public boolean complete() {
        return step == DONE && loose == null;
    }

    public void setFrame(String frame) {
        this.frame = frame;
        step = 1;
        changed();
    }

    public void setTool(Item tool) {
        this.tool = tool;
        step = 3;
        changed();
    }

    public void setStep(int step) {
        this.step = step;
        changed();
    }

    public void setBattery(int energy) {
        this.energy = energy;
        step = 7;
        changed();
    }

    public void setLoose(@Nullable Upgrade upgrade) {
        loose = upgrade;
        changed();
    }

    /** Screws in whatever is loose. False when nothing is. */
    public boolean fix() {
        if (loose != null) {
            upgrades.add(loose);
            loose = null;
        } else if (step % 2 == 1) {
            step++;
        } else {
            return false;
        }
        changed();
        return true;
    }

    /** Takes the outermost part off and returns it, or empty when the stand is bare. */
    public ItemStack strip() {
        final ItemStack part;
        if (loose != null) {
            part = new ItemStack(loose.item());
            loose = null;
        } else if (step == DONE && !upgrades.isEmpty()) {
            part = new ItemStack(upgrades.removeLast().item());
            energy = Math.min(energy, PoweredToolItem.CAPACITY * (1 + Upgrade.count(upgrades, Upgrade.ENERGY_CELL)));
        } else if (step >= 7) {
            part = ToolParts.battery(energy);
            energy = 0;
            step = 6;
        } else if (step >= 5) {
            part = new ItemStack(SimpleMachines.controlBoard);
            step = 4;
        } else if (step >= 3) {
            part = new ItemStack(ToolParts.core(tool));
            tool = null;
            step = 2;
        } else if (step >= 1) {
            part = new ItemStack(ToolParts.frameItem(frame));
            frame = "";
            step = 0;
        } else {
            return ItemStack.EMPTY;
        }
        changed();
        return part;
    }

    /** The finished tool as it would come off the stand. */
    public ItemStack build() {
        if (tool == null) return ItemStack.EMPTY;
        final ItemStack stack = new ItemStack(tool);
        stack.set(SimpleMachines.TOOL_FRAME, frame.isEmpty() ? "iron" : frame);
        final List<Upgrade> sorted = new ArrayList<>(upgrades);
        sorted.sort(Comparator.comparingInt(Enum::ordinal));
        if (!sorted.isEmpty()) stack.set(SimpleMachines.UPGRADES_FITTED, Upgrade.join(sorted));
        if (energy > 0) stack.set(SimpleMachines.ENERGY, Math.min(energy, PoweredToolItem.capacity(stack)));
        return stack;
    }

    /** Lifts the finished tool off, leaving the stand bare. */
    public ItemStack take() {
        final ItemStack stack = build();
        clear();
        return stack;
    }

    /** Puts a finished tool back on the stand, parts and charge intact. */
    public void mount(ItemStack stack) {
        tool = stack.getItem();
        frame = PoweredToolItem.steel(stack) ? "steel" : "iron";
        upgrades = new ArrayList<>(PoweredToolItem.upgrades(stack));
        energy = PoweredToolItem.energy(stack);
        loose = null;
        step = DONE;
        changed();
    }

    private void clear() {
        step = 0;
        frame = "";
        tool = null;
        energy = 0;
        upgrades = new ArrayList<>();
        loose = null;
        changed();
    }

    /** Everything on the stand as loose items, for when it is broken. */
    public List<ItemStack> contents() {
        final List<ItemStack> out = new ArrayList<>();
        if (complete()) {
            out.add(build());
            return out;
        }
        if (step >= 1) out.add(new ItemStack(ToolParts.frameItem(frame)));
        if (step >= 3) out.add(new ItemStack(ToolParts.core(tool)));
        if (step >= 5) out.add(new ItemStack(SimpleMachines.controlBoard));
        if (step >= 7) out.add(ToolParts.battery(energy));
        for (Upgrade u : upgrades) out.add(new ItemStack(u.item()));
        if (loose != null) out.add(new ItemStack(loose.item()));
        out.removeIf(ItemStack::isEmpty);
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        read(input.getIntOr("step", 0), input.getStringOr("frame", ""), input.getStringOr("tool", ""), input.getIntOr("energy", 0),
                input.getStringOr("upgrades", ""), input.getStringOr("loose_upgrade", ""));
    }

    private void read(int step, String frame, String tool, int energy, String upgrades, String loose) {
        this.step = step;
        this.frame = frame;
        final Identifier id = Identifier.tryParse(tool);
        final Item item = id == null || tool.isEmpty() ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        this.tool = item == Items.AIR ? null : item;
        this.energy = energy;
        this.upgrades = Upgrade.parse(upgrades);
        this.loose = Upgrade.byId(loose);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("step", step);
        output.putString("frame", frame);
        output.putString("tool", tool == null ? "" : BuiltInRegistries.ITEM.getKey(tool).toString());
        output.putInt("energy", energy);
        output.putString("upgrades", Upgrade.join(upgrades));
        output.putString("loose_upgrade", loose == null ? "" : loose.id);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("step", step);
        tag.putString("frame", frame);
        tag.putString("tool", tool == null ? "" : BuiltInRegistries.ITEM.getKey(tool).toString());
        tag.putInt("energy", energy);
        tag.putString("upgrades", Upgrade.join(upgrades));
        tag.putString("loose_upgrade", loose == null ? "" : loose.id);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
