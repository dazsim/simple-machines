package com.workshopcraft.simplemachines.compat.jei;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Core;
import com.workshopcraft.simplemachines.machine.LavaGeneratorBlockEntity;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Lava generator: one bucket of lava per chassis tier. */
class LavaCategory extends Base<LavaCategory.Burn> {
    enum Burn {
        IRON(LavaGeneratorBlockEntity.PER_TICK, "iron_chassis"), STEEL(LavaGeneratorBlockEntity.STEEL_PER_TICK, "steel_chassis");

        final int perTick;
        final String chassis;

        Burn(int perTick, String chassis) {
            this.perTick = perTick;
            this.chassis = chassis;
        }
    }

    static final IRecipeType<Burn> TYPE = IRecipeType.create(SimpleMachines.id("lava_generator"), Burn.class);

    LavaCategory(IGuiHelper gui) {
        super(TYPE, "lava_generator", SimpleMachines.MACHINES.get(Core.LAVA), gui, 176, 2);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, Burn burn, IFocusGroup focuses) {
        b.addInputSlot(1, ROW).setStandardSlotBackground().add(new ItemStack(Items.LAVA_BUCKET));
        b.addOutputSlot(66, ROW).setStandardSlotBackground().add(new ItemStack(Items.BUCKET));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder b, Burn burn, IFocusGroup focuses) {
        final int ticks = 1000 * LavaGeneratorBlockEntity.TICKS_PER_MB;
        arrow(b, 28, ticks);
        line(b, 0, Component.translatable("jei.simplemachines.makes", Component.translatable("jei.simplemachines." + burn.chassis),
                JeiText.fe((long) ticks * burn.perTick)), JeiText.perTick(burn.perTick));
        line(b, 1, Component.translatable("jei.simplemachines.lava_note", JeiText.seconds(ticks)));
    }
}
