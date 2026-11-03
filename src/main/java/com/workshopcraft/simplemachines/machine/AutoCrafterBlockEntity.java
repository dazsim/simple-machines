package com.workshopcraft.simplemachines.machine;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crafts the 3x3 ghost pattern with any crafting recipe.
 * Slots 0-8 buffer one ingredient group each, 9-12 hold the result and remainders.
 */
public class AutoCrafterBlockEntity extends WorkBlockEntity {
    public static final int GRID = 9, OUTPUTS = 4;
    public static final int CAPACITY = 10_000, MAX_IN = 200, PER_TICK = 20, TIME = 100;
    public static final int DATA_COUNT = EnergyMenu.DATA_COUNT + 3;
    public static final int ALWAYS = 0, WITH_SIGNAL = 1, WITHOUT_SIGNAL = 2;
    public static final int IDLE = 0, NO_RECIPE = 1, WORKING = 2, MISSING = 3, FULL = 4, NO_POWER = 5, OFF = 6;
    private static final int SETTLE = 10;

    /** Ghost pattern in 0-8, result preview in 9. */
    public final SimpleContainer pattern = new SimpleContainer(GRID + 1) {
        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return AutoCrafterBlockEntity.this.stillValid(player);
        }
    };
    private final int[] group = new int[GRID];
    private int groups;
    private int redstone = ALWAYS;
    private int status = IDLE;
    private int settle;
    private boolean stale = true;
    private @Nullable RecipeHolder<CraftingRecipe> recipe;
    private final List<Map<Item, Boolean>> fitCache = new ArrayList<>();

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id - EnergyMenu.DATA_COUNT) {
                case 0 -> redstone;
                case 1 -> groups;
                case 2 -> status;
                default -> data.get(id);
            };
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

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleMachines.MACHINE_ENTITIES.get(com.workshopcraft.simplemachines.assembly.Core.CRAFTER), pos, state, GRID + OUTPUTS, CAPACITY, MAX_IN, 0);
        for (int i = 0; i < GRID; i++) fitCache.add(new HashMap<>());
        regroup();
    }

    @Override
    protected boolean isOutput(int slot) {
        return slot >= GRID;
    }

    @Override
    public boolean energyOut(net.minecraft.core.@Nullable Direction side) {
        return false;
    }

    public int groups() {
        return groups;
    }

    public int redstone() {
        return redstone;
    }

    public void cycleRedstone(boolean back) {
        redstone = (redstone + (back ? 2 : 1)) % 3;
        setChanged();
    }

    public void setGhost(int pos, ItemStack stack) {
        if (pos < 0 || pos >= GRID) return;
        final ItemStack ghost = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.isSameItemSameComponents(ghost, pattern.getItem(pos)) && ghost.isEmpty() == pattern.getItem(pos).isEmpty()) return;
        pattern.setItem(pos, ghost);
        changed();
    }

    public void clearPattern() {
        for (int i = 0; i < GRID; i++) pattern.setItem(i, ItemStack.EMPTY);
        changed();
    }

    private void changed() {
        regroup();
        settle = SETTLE;
        progress = 0;
        setChanged();
        if (level instanceof ServerLevel server) refresh(server);
    }

    /** Groups grid positions by item; unstackables get one buffer slot per position. */
    private void regroup() {
        Arrays.fill(group, -1);
        groups = 0;
        final int[] size = new int[GRID];
        for (int p = 0; p < GRID; p++) {
            final ItemStack g = pattern.getItem(p);
            if (g.isEmpty()) continue;
            if (g.getMaxStackSize() > 1) {
                for (int q = 0; q < p; q++) {
                    if (group[q] >= 0 && ItemStack.isSameItemSameComponents(pattern.getItem(q), g) && size[group[q]] < g.getMaxStackSize()) {
                        group[p] = group[q];
                        break;
                    }
                }
            }
            if (group[p] < 0) group[p] = groups++;
            size[group[p]]++;
        }
        for (Map<Item, Boolean> m : fitCache) m.clear();
        stale = true;
    }

    private List<ItemStack> ghosts() {
        final List<ItemStack> list = new ArrayList<>(GRID);
        for (int p = 0; p < GRID; p++) list.add(pattern.getItem(p).copy());
        return list;
    }

    private void refresh(ServerLevel level) {
        if (!stale) return;
        stale = false;
        final CraftingInput input = CraftingInput.of(3, 3, ghosts());
        recipe = groups == 0 ? null : CrafterBlock.getPotentialResults(level, input).orElse(null);
        pattern.setItem(GRID, recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input));
    }

    private boolean fitsGroup(ServerLevel level, int slot, ItemStack stack) {
        if (slot >= groups) return false;
        refresh(level);
        if (recipe == null) return false;
        final RecipeHolder<CraftingRecipe> r = recipe;
        return fitCache.get(slot).computeIfAbsent(stack.getItem(), item -> {
            final List<ItemStack> list = ghosts();
            for (int p = 0; p < GRID; p++) if (group[p] == slot) list.set(p, stack.copyWithCount(1));
            return r.value().matches(CraftingInput.of(3, 3, list), level);
        });
    }

    @Override
    protected boolean accepts(ServerLevel level, int slot, ItemStack stack) {
        if (slot >= GRID) return false;
        final ItemStack cur = items.get(slot);
        if (!cur.isEmpty()) return ItemStack.isSameItemSameComponents(cur, stack);
        return fitsGroup(level, slot, stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize();
    }

    @Override
    protected void work(ServerLevel level, BlockPos pos, BlockState state, Tier tier) {
        refresh(level);
        if (settle > 0 && --settle == 0) relocate(level);
        total = tier.time(TIME);
        status = step(level, pos);
        setLit(level, pos, state, status == WORKING);
    }

    private int step(ServerLevel level, BlockPos pos) {
        if (groups == 0) return idle(IDLE);
        if (recipe == null) return idle(NO_RECIPE);
        final boolean signal = level.hasNeighborSignal(pos);
        if (redstone == WITH_SIGNAL && !signal || redstone == WITHOUT_SIGNAL && signal) return OFF;
        if (!stocked()) return idle(MISSING);
        final Craft craft = plan(level);
        if (craft == null) return idle(MISSING);
        if (!craft.fits) return FULL;
        if (progress >= total) {
            finish(level, craft);
            progress = 0;
            setChanged();
            return WORKING;
        }
        if (energy.energy < PER_TICK) return NO_POWER;
        energy.energy -= PER_TICK;
        progress++;
        setChanged();
        return WORKING;
    }

    private int idle(int why) {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
        return why;
    }

    private boolean stocked() {
        final int[] need = new int[GRID];
        for (int p = 0; p < GRID; p++) if (group[p] >= 0) need[group[p]]++;
        for (int g = 0; g < groups; g++) if (items.get(g).getCount() < need[g]) return false;
        return true;
    }

    private record Craft(ItemStack result, List<ItemStack> extra, boolean fits) {}

    private @Nullable Craft plan(ServerLevel level) {
        final List<ItemStack> in = new ArrayList<>(GRID);
        for (int p = 0; p < GRID; p++) in.add(group[p] < 0 ? ItemStack.EMPTY : items.get(group[p]).copyWithCount(1));
        final CraftingInput input = CraftingInput.of(3, 3, in);
        if (recipe == null || !recipe.value().matches(input, level)) return null;
        final ItemStack result = recipe.value().assemble(input);
        if (result.isEmpty()) return null;
        final List<ItemStack> extra = new ArrayList<>();
        for (ItemStack s : recipe.value().getRemainingItems(input)) if (!s.isEmpty()) extra.add(s);
        final NonNullList<ItemStack> sim = NonNullList.withSize(OUTPUTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUTS; i++) sim.set(i, items.get(GRID + i).copy());
        boolean fits = put(sim, result.copy());
        for (ItemStack s : extra) fits &= put(sim, s.copy());
        return new Craft(result, extra, fits);
    }

    private static boolean put(NonNullList<ItemStack> outs, ItemStack stack) {
        for (ItemStack out : outs) {
            if (stack.isEmpty()) break;
            if (!out.isEmpty() && ItemStack.isSameItemSameComponents(out, stack)) {
                final int n = Math.min(stack.getCount(), out.getMaxStackSize() - out.getCount());
                out.grow(n);
                stack.shrink(n);
            }
        }
        for (int i = 0; i < outs.size() && !stack.isEmpty(); i++) {
            if (outs.get(i).isEmpty()) {
                outs.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack.isEmpty();
    }

    private void finish(ServerLevel level, Craft craft) {
        for (int p = 0; p < GRID; p++) if (group[p] >= 0) items.get(group[p]).shrink(1);
        craft.result.onCraftedBySystem(level);
        store(craft.result.copy());
        for (ItemStack s : craft.extra) store(s.copy());
    }

    /** Moves buffer items that no longer fit the pattern to a matching slot or the outputs, never deleting. */
    private void relocate(ServerLevel level) {
        final List<ItemStack> loose = new ArrayList<>();
        for (int i = 0; i < GRID; i++) {
            final ItemStack s = items.get(i);
            if (!s.isEmpty() && !fitsGroup(level, i, s)) {
                loose.add(s);
                items.set(i, ItemStack.EMPTY);
            }
        }
        if (loose.isEmpty()) return;
        for (ItemStack s : loose) {
            for (int i = 0; i < groups && !s.isEmpty(); i++) {
                final ItemStack cur = items.get(i);
                if (cur.isEmpty() ? fitsGroup(level, i, s) : ItemStack.isSameItemSameComponents(cur, s)) {
                    final int n = Math.min(s.getCount(), s.getMaxStackSize() - cur.getCount());
                    if (cur.isEmpty()) items.set(i, s.split(n));
                    else {
                        cur.grow(n);
                        s.shrink(n);
                    }
                }
            }
            if (!s.isEmpty()) store(s);
            for (int i = 0; i < GRID && !s.isEmpty(); i++) {
                if (items.get(i).isEmpty()) {
                    items.set(i, s.copy());
                    s.setCount(0);
                }
            }
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        redstone = input.getIntOr("redstone", ALWAYS);
        final NonNullList<ItemStack> list = NonNullList.withSize(GRID, ItemStack.EMPTY);
        input.read("pattern", ItemContainerContents.CODEC).ifPresent(c -> c.copyInto(list));
        for (int i = 0; i < GRID; i++) pattern.setItem(i, list.get(i));
        regroup();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("redstone", redstone);
        output.store("pattern", ItemContainerContents.CODEC, patternContents());
    }

    private ItemContainerContents patternContents() {
        final List<ItemStack> list = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) list.add(pattern.getItem(i));
        return ItemContainerContents.fromItems(list);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (groups > 0) components.set(SimpleMachines.CRAFTER_PATTERN, patternContents());
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        final ItemContainerContents c = components.get(SimpleMachines.CRAFTER_PATTERN);
        if (c == null) return;
        final NonNullList<ItemStack> list = NonNullList.withSize(GRID, ItemStack.EMPTY);
        c.copyInto(list);
        for (int i = 0; i < GRID; i++) pattern.setItem(i, list.get(i));
        regroup();
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("pattern");
    }

    @Override
    protected net.minecraft.network.chat.Component getDefaultName() {
        return net.minecraft.network.chat.Component.translatable("container.simplemachines.auto_crafter");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        if (level instanceof ServerLevel server) refresh(server);
        return new AutoCrafterMenu(id, inventory, this, pattern, menuData, this);
    }
}
