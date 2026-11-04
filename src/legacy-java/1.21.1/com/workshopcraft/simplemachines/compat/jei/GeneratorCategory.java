package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.GeneratorBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if >=26.3 {
/*import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
*///?}
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Solid fuels grouped by burn time, with the FE each gives. */
class GeneratorCategory extends Base<GeneratorCategory.Fuel> {
    record Fuel(List<ItemStack> items, int burn) {
    }

    static final RecipeType<Fuel> TYPE = new RecipeType<>(SimpleMachines.id("solid_fuel_generator"), Fuel.class);

    GeneratorCategory(IGuiHelper gui) {
        super(TYPE, "generator", SimpleMachines.generator, gui, 176, 3);
    }

    static List<Fuel> fuels() {
        final Level level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        final Map<Integer, List<ItemStack>> byBurn = new TreeMap<>((a, b) -> b - a);
        for (Item item : BuiltInRegistries.ITEM) {
            final ItemStack stack = new ItemStack(item);
            if (!GeneratorBlockEntity.isSolidFuel(level, stack)) continue;
            byBurn.computeIfAbsent(burn(level, stack), k -> new ArrayList<>()).add(stack);
        }
        final List<Fuel> out = new ArrayList<>();
        byBurn.forEach((burn, items) -> out.add(new Fuel(items, burn)));
        return out;
    }

    //? if >=26.3 {
    /*private static final ContextKeySet JEI_SET = new ContextKeySet.Builder().required(LootContextParams.BLOCK_STATE).optional(LootContextParams.ORIGIN).build();
    // vanilla normal furnace burn times, used when a remote server's fuel data is not available
    private static final Map<String, Integer> VANILLA = Map.ofEntries(Map.entry("cooking/time_bamboo", 50), Map.entry("cooking/time_blaze_rod", 2400),
            Map.entry("cooking/time_boats", 1200), Map.entry("cooking/time_coal", 1600), Map.entry("cooking/time_coal_block", 16000),
            Map.entry("cooking/time_dried_kelp_block", 4001), Map.entry("cooking/time_dry_plants", 100), Map.entry("cooking/time_hanging_signs", 800),
            Map.entry("cooking/time_lava_bucket", 20000), Map.entry("cooking/time_roots", 300), Map.entry("cooking/time_wood_blocks", 300),
            Map.entry("cooking/time_wood_items_extra_small", 100), Map.entry("cooking/time_wood_items_large", 200),
            Map.entry("cooking/time_wood_items_small", 300), Map.entry("cooking/time_wood_slabs", 150), Map.entry("cooking/time_wool", 100),
            Map.entry("cooking/time_wool_carpets", 67), Map.entry("cooking/time_wool_slabs", 50), Map.entry("cooking/time_biofuel", 2400));
    *///?}

    private static int burn(Level level, ItemStack stack) {
        //? if >=26.3 {
        /*final CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
        if (fuel == null) return 0;
        final MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            final LootContext ctx = new LootContext.Builder(new LootParams.Builder(server.overworld())
                    .withParameter(LootContextParams.BLOCK_STATE, SimpleMachines.generator.defaultBlockState()).create(JEI_SET)).create(Optional.empty());
            return fuel.burnTime().get(ctx, 0);
        }
        return switch (fuel.burnTime()) {
            case ResolvableInt.Constant c -> c.value();
            case ResolvableInt.Reference r -> VANILLA.getOrDefault(r.key().identifier().getPath(), 0);
        };
        *///?} else {
        return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        //?}
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Fuel fuel, IFocusGroup focuses) {
        b.addInputSlot(1, ROW).setStandardSlotBackground().addItemStacks(fuel.items());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Fuel fuel, IFocusGroup focuses) {
        b.addAnimatedRecipeFlameWidget(fuel.burn()).setPosition(24, ROW + 1);
        b.addText(Component.translatable("jei.simplemachines.burns", JeiText.seconds(fuel.burn())), 100, 10).setColor(TEXT).setPosition(44, ROW + 4);
        line(b, 0, gen("jei.simplemachines.iron_chassis", fuel.burn(), GeneratorBlockEntity.PER_TICK), JeiText.perTick(GeneratorBlockEntity.PER_TICK));
        line(b, 1, gen("jei.simplemachines.steel_chassis", fuel.burn(), GeneratorBlockEntity.STEEL_PER_TICK), JeiText.perTick(GeneratorBlockEntity.STEEL_PER_TICK));
        line(b, 2, Component.translatable("jei.simplemachines.generator_note"));
    }

    private static Component gen(String chassis, int burn, int perTick) {
        return Component.translatable("jei.simplemachines.makes", Component.translatable(chassis), JeiText.fe((long) burn * perTick));
    }
}
