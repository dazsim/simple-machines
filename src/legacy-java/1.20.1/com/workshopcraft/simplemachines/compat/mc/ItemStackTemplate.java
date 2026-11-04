package com.workshopcraft.simplemachines.compat.mc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.compat.mc.codec.StreamCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Stand-in for 26.x ItemStackTemplate: an immutable result stack, json {id, count}. */
public record ItemStackTemplate(ItemStack stack) {
    public static final Codec<ItemStackTemplate> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(ItemStackTemplate::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count)
    ).apply(i, (item, count) -> new ItemStackTemplate(new ItemStack(item, count))));
    public static final StreamCodec<Object, ItemStackTemplate> STREAM_CODEC =
            StreamCodec.of(b -> new ItemStackTemplate(b.readItem()), (FriendlyByteBuf b, ItemStackTemplate t) -> b.writeItem(t.stack));

    public ItemStackTemplate(Item item) {
        this(new ItemStack(item));
    }

    public ItemStack create() {
        return stack.copy();
    }

    public Item item() {
        return stack.getItem();
    }

    public int count() {
        return stack.getCount();
    }
}
