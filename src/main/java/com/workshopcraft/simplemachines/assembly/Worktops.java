package com.workshopcraft.simplemachines.assembly;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Worktops are shared with other mods through the c:worktops tag and stored by item id. */
public final class Worktops {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "worktops"));
    public static final TagKey<Item> SCREWDRIVERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "tools/screwdriver"));
    public static final String DEFAULT = "simplemachines:oak_worktop";

    private Worktops() {
    }

    public interface Holder {
        String worktop();

        void setWorktop(String id);
    }

    /** The worktop id of a stack, or empty when it is not a worktop. */
    public static String of(ItemStack stack) {
        return stack.is(TAG) ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : "";
    }

    public static ItemStack stack(String id) {
        final Identifier key = Identifier.tryParse(id);
        if (id.isEmpty() || key == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }
}
