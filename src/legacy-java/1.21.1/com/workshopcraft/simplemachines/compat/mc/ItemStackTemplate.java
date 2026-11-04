package com.workshopcraft.simplemachines.compat.mc;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Stand-in for 26.x net.minecraft.world.item.ItemStackTemplate: an immutable result stack. */
public record ItemStackTemplate(ItemStack stack) {
    public static final Codec<ItemStackTemplate> CODEC = ItemStack.STRICT_CODEC.xmap(ItemStackTemplate::new, ItemStackTemplate::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ItemStackTemplate::new, ItemStackTemplate::stack);

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
