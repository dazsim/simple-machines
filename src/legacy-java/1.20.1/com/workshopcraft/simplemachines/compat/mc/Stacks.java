package com.workshopcraft.simplemachines.compat.mc;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** 1.20.5+ ItemStack helpers. */
public final class Stacks {
    public static final TagKey<Block> INCORRECT_FOR_NETHERITE_TOOL = TagKey.create(Registries.BLOCK, new ResourceLocation("simplemachines", "incorrect_for_netherite_tool"));
    public static final Codec<ItemStack> OPTIONAL_CODEC = ItemStack.CODEC;

    private Stacks() {
    }

    public static void consume(ItemStack stack, int count, @Nullable LivingEntity entity) {
        if (!(entity instanceof Player p) || !p.getAbilities().instabuild) stack.shrink(count);
    }

    public static int hash(ItemStack stack) {
        return Objects.hash(stack.getItem(), stack.getTag());
    }

    public static void limit(Container container, ItemStack stack) {
        final int max = container instanceof CompatContainerBlockEntity c ? c.getMaxStackSize(stack) : Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        if (stack.getCount() > max) stack.setCount(max);
    }
}
