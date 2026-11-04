package com.workshopcraft.simplemachines.mixin;

import java.util.Collection;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.1 walks a multipart's selectors once per block state while loading; do it once per multipart. */
@Mixin(MultiPart.class)
public abstract class MultiPartLoadMixin {
    @Unique private Collection<ResourceLocation> simplemachines$deps;
    @Unique private boolean simplemachines$resolved;

    @Inject(method = {"getDependencies", "method_4755"}, remap = false, at = @At("HEAD"), cancellable = true)
    private void simplemachines$deps(CallbackInfoReturnable<Collection<ResourceLocation>> cir) {
        if (simplemachines$deps != null) cir.setReturnValue(simplemachines$deps);
    }

    @Inject(method = {"getDependencies", "method_4755"}, remap = false, at = @At("RETURN"))
    private void simplemachines$depsStore(CallbackInfoReturnable<Collection<ResourceLocation>> cir) {
        simplemachines$deps = cir.getReturnValue();
    }

    @Inject(method = {"resolveParents", "method_45785"}, remap = false, at = @At("HEAD"), cancellable = true)
    private void simplemachines$resolve(Function<ResourceLocation, UnbakedModel> f, CallbackInfo ci) {
        if (simplemachines$resolved) ci.cancel();
        simplemachines$resolved = true;
    }
}
