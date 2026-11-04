package com.workshopcraft.simplemachines.forge.mixin;

import java.util.function.Predicate;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 rebuilds every multipart predicate once per block state while loading; the chassis has thousands of both. */
@Mixin(Selector.class)
public abstract class SelectorMixin {
    @Unique private StateDefinition<Block, BlockState> simplemachines$def;
    @Unique private Predicate<BlockState> simplemachines$pred;

    @Inject(method = "getPredicate", at = @At("HEAD"), cancellable = true)
    private void simplemachines$cached(StateDefinition<Block, BlockState> def, CallbackInfoReturnable<Predicate<BlockState>> cir) {
        if (simplemachines$def == def && simplemachines$pred != null) cir.setReturnValue(simplemachines$pred);
    }

    @Inject(method = "getPredicate", at = @At("RETURN"))
    private void simplemachines$store(StateDefinition<Block, BlockState> def, CallbackInfoReturnable<Predicate<BlockState>> cir) {
        simplemachines$def = def;
        simplemachines$pred = cir.getReturnValue();
    }
}
