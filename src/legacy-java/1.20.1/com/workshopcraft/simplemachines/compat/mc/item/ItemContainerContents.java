package com.workshopcraft.simplemachines.compat.mc.item;

import com.mojang.serialization.Codec;
import com.workshopcraft.simplemachines.compat.mc.codec.ByteBufCodecs;
import com.workshopcraft.simplemachines.compat.mc.codec.StreamCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Stand-in for 1.20.5+ ItemContainerContents: a slot-indexed list of stacks. */
public final class ItemContainerContents {
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());
    public static final Codec<ItemContainerContents> CODEC = ItemStack.CODEC.listOf().xmap(ItemContainerContents::new, c -> c.items);
    public static final StreamCodec<Object, ItemContainerContents> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final List<ItemStack> items;

    private ItemContainerContents(List<ItemStack> items) {
        this.items = items;
    }

    public static ItemContainerContents fromItems(List<ItemStack> items) {
        final List<ItemStack> l = new ArrayList<>(items.size());
        for (ItemStack s : items) l.add(s.copy());
        return new ItemContainerContents(l);
    }

    public void copyInto(NonNullList<ItemStack> out) {
        for (int i = 0; i < out.size(); i++) out.set(i, i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
    }

    public Stream<ItemStack> stream() {
        return items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyItems() {
        return items.stream().filter(s -> !s.isEmpty());
    }
}
