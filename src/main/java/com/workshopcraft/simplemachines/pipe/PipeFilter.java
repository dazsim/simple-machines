package com.workshopcraft.simplemachines.pipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Settings of one Pipe Filter: up to nine sample items, allow or deny, and whether tags and item data count. */
public final class PipeFilter {
    public static final int SIZE = 9;
    public static final PipeFilter EMPTY = new PipeFilter(List.of(), false, false, false);
    public static final Codec<PipeFilter> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(f -> f.items),
            Codec.BOOL.optionalFieldOf("deny", false).forGetter(f -> f.deny),
            Codec.BOOL.optionalFieldOf("tags", false).forGetter(f -> f.tags),
            Codec.BOOL.optionalFieldOf("data", false).forGetter(f -> f.data)
    ).apply(i, PipeFilter::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeFilter> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final List<ItemStack> items;
    public final boolean deny, tags, data;
    private List<Fluid> fluids;

    public PipeFilter(List<ItemStack> items, boolean deny, boolean tags, boolean data) {
        final List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) list.add(i < items.size() ? items.get(i).copyWithCount(1) : ItemStack.EMPTY);
        if (items.size() > SIZE) throw new IllegalArgumentException("too many filter items");
        this.items = List.copyOf(list);
        this.deny = deny;
        this.tags = tags;
        this.data = data;
    }

    public ItemStack item(int slot) {
        return items.get(slot).copy();
    }

    public PipeFilter withItem(int slot, ItemStack stack) {
        final List<ItemStack> list = new ArrayList<>(items);
        list.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        return new PipeFilter(list, deny, tags, data);
    }

    public PipeFilter toggle(int setting) {
        return new PipeFilter(items, deny ^ setting == 0, tags ^ setting == 1, data ^ setting == 2);
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    public List<ItemStack> samples() {
        return items.stream().filter(s -> !s.isEmpty()).toList();
    }

    /** Whether the stack is one of the listed items, ignoring allow or deny. */
    public boolean matches(ItemStack stack) {
        for (ItemStack sample : items) {
            if (sample.isEmpty()) continue;
            if (data ? ItemStack.isSameItemSameComponents(sample, stack) : ItemStack.isSameItem(sample, stack)) return true;
            if (tags && sharesTag(sample, stack)) return true;
        }
        return false;
    }

    public boolean accepts(ItemStack stack) {
        return matches(stack) != deny;
    }

    public boolean matchesFluid(Fluid fluid) {
        for (Fluid f : fluids()) {
            if (f.isSame(fluid)) return true;
            if (tags && f.builtInRegistryHolder().tags().anyMatch(fluid::is)) return true;
        }
        return false;
    }

    public boolean acceptsFluid(Fluid fluid) {
        return matchesFluid(fluid) != deny;
    }

    private List<Fluid> fluids() {
        if (fluids == null) {
            fluids = new ArrayList<>();
            for (ItemStack sample : items) {
                if (sample.isEmpty()) continue;
                final Fluid f = sample.getItem() instanceof BucketItem bucket ? bucket.getContent() : Transfer.itemFluid.fluid(sample);
                if (f != null && f != Fluids.EMPTY) fluids.add(f);
            }
        }
        return fluids;
    }

    /** Material tags such as c:ingots/iron when the sample has any, otherwise any tag the two share. */
    private static boolean sharesTag(ItemStack sample, ItemStack stack) {
        final Holder<Item> a = sample.typeHolder();
        final List<TagKey<Item>> all = a.tags().toList();
        final List<TagKey<Item>> material = all.stream()
                .filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().contains("/")).toList();
        for (TagKey<Item> t : material.isEmpty() ? all : material) if (stack.is(t)) return true;
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PipeFilter f) || f.deny != deny || f.tags != tags || f.data != data) return false;
        for (int i = 0; i < SIZE; i++) {
            final ItemStack x = items.get(i), y = f.items.get(i);
            if (x.isEmpty() != y.isEmpty() || !x.isEmpty() && !ItemStack.isSameItemSameComponents(x, y)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = Objects.hash(deny, tags, data);
        for (ItemStack s : items) h = h * 31 + (s.isEmpty() ? 0 : ItemStack.hashItemAndComponents(s));
        return h;
    }

    @Override
    public String toString() {
        return (deny ? "deny " : "allow ") + samples().stream().map(s -> s.getItem().toString()).collect(Collectors.joining(","));
    }
}
