package com.workshopcraft.simplemachines.forge.mixin;

import java.util.function.Function;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 bakes a multipart once per block state; the chassis has tens of thousands of states, so bake ours once. */
@Mixin(MultiPart.class)
public abstract class MultiPartMixin {
    @Shadow @Final private StateDefinition<Block, BlockState> definition;
    @Unique private BakedModel simplemachines$baked;
    @Unique private ModelState simplemachines$state;

    @Inject(method = "bake", at = @At("HEAD"), cancellable = true)
    private void simplemachines$cached(ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ResourceLocation location, CallbackInfoReturnable<BakedModel> cir) {
        if (simplemachines$baked != null && simplemachines$state == state) cir.setReturnValue(simplemachines$baked);
    }

    @Inject(method = "bake", at = @At("RETURN"))
    private void simplemachines$store(ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ResourceLocation location, CallbackInfoReturnable<BakedModel> cir) {
        if (simplemachines$baked == null && "simplemachines".equals(BuiltInRegistries.BLOCK.getKey(definition.getOwner()).getNamespace())) {
            simplemachines$baked = cir.getReturnValue();
            simplemachines$state = state;
        }
    }
}
