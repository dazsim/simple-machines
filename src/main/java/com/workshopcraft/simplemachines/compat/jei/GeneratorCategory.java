package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.machine.GeneratorBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Solid fuels grouped by burn time, with the FE each gives. */
class GeneratorCategory extends Base<GeneratorCategory.Fuel> {
    record Fuel(List<ItemStack> items, int burn) {
    }

    static final IRecipeType<Fuel> TYPE = IRecipeType.create(SimpleMachines.id("solid_fuel_generator"), Fuel.class);

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
            byBurn.computeIfAbsent(level.fuelValues().burnDuration(stack), k -> new ArrayList<>()).add(stack);
        }
        final List<Fuel> out = new ArrayList<>();
        byBurn.forEach((burn, items) -> out.add(new Fuel(items, burn)));
        return out;
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
