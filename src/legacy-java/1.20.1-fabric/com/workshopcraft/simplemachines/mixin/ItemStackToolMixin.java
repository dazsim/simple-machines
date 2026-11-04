package com.workshopcraft.simplemachines.mixin;

import com.workshopcraft.simplemachines.item.PoweredToolItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 Fabric asks the item about drops without the stack, so an unpowered drill or chainsaw still dropped blocks. */
@Mixin(ItemStack.class)
public abstract class ItemStackToolMixin {
    @Inject(method = {"isCorrectToolForDrops", "method_7951"}, remap = false, at = @At("HEAD"), cancellable = true)
    private void simplemachines$poweredDrops(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        final ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof PoweredToolItem tool) cir.setReturnValue(tool.isCorrectToolForDrops(self, state));
    }
}
