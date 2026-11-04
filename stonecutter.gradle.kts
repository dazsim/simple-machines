plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    replacements.string(current.parsed < "1.21.11") {
        replace("Identifier", "ResourceLocation")
    }

    // 26.x useItemOn returns InteractionResult; 1.21.1 gets a bridge to ItemInteractionResult
    replacements.regex(current.parsed < "1.21.2") {
        replace(
            "protected InteractionResult useItemOn\\(",
            "protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack s$, BlockState b$, Level l$, BlockPos p$, Player e$, InteractionHand h$, BlockHitResult r$) { return com.workshopcraft.simplemachines.compat.mc.Interactions.item(useItemOn$(s$, b$, l$, p$, e$, h$, r$)); }\n    protected InteractionResult useItemOn$(",
            "protected net\\.minecraft\\.world\\.ItemInteractionResult useItemOn\\(.*\\n    protected InteractionResult useItemOn\\$\\(",
            "protected InteractionResult useItemOn("
        )
        replace(
            "@Override\\n    protected void affectNeighborsAfterRemoval\\(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston\\) \\{",
            "@Override\n    protected void onRemove(BlockState state, net.minecraft.world.level.Level l$, BlockPos pos, BlockState n$, boolean movedByPiston) { com.workshopcraft.simplemachines.compat.mc.Removal.onRemove(l$, pos, state, n$); super.onRemove(state, l$, pos, n$, movedByPiston); if (!state.is(n$.getBlock()) && l$ instanceof ServerLevel s$) affectNeighborsAfterRemoval(state, s$, pos, movedByPiston); }\n    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {",
            "@Override\\n    protected void onRemove\\(BlockState state, net\\.minecraft.*\\n    protected void affectNeighborsAfterRemoval\\(",
            "@Override\n    protected void affectNeighborsAfterRemoval("
        )
    }

    // 1.21.1 stand-ins live in compat.mc under src/legacy-java/1.21.1
    replacements.string(current.parsed < "1.21.2") {
        val mc = "com.workshopcraft.simplemachines.compat.mc"
        replace("net.minecraft.world.level.storage.ValueInput", "$mc.ValueInput")
        replace("net.minecraft.world.level.storage.ValueOutput", "$mc.ValueOutput")
        replace("net.minecraft.core.component.DataComponentGetter", "$mc.DataComponentGetter")
        replace("ContainerHelper.loadAllItems(", "$mc.ItemContainers.loadAllItems(")
        replace("ContainerHelper.saveAllItems(", "$mc.ItemContainers.saveAllItems(")
        replace("extends BlockEntity {", "extends $mc.CompatBlockEntity {")
        replace("extends BlockEntity implements", "extends $mc.CompatBlockEntity implements")
        replace("extends BaseContainerBlockEntity ", "extends $mc.CompatContainerBlockEntity ")
        replace("net.minecraft.world.inventory.ContainerInput", "net.minecraft.world.inventory.ClickType")
        replace("ContainerInput ", "ClickType ")
        replace("ContainerInput.", "ClickType.")
        replace("extends AbstractContainerMenu ", "extends $mc.CompatMenu ")
        replace("player.sendOverlayMessage(", "$mc.Players.overlay(player, ")
        replace("p.sendOverlayMessage(", "$mc.Players.overlay(p, ")
        replace("net.minecraft.world.level.saveddata.SavedDataType", "$mc.SavedDataType")
        replace("extends SavedData {", "extends $mc.CodecSavedData {")
        replace("server.getDataStorage().computeIfAbsent(TYPE)", "TYPE.get(server)")
        replace("Containers.updateNeighboursAfterDestroy(state, level, pos);", "((net.minecraft.world.level.Level) level).updateNeighbourForOutputSignal(pos, state.getBlock());")
        replace("getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction)", "getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)")
        replace("protected boolean propagatesSkylightDown(BlockState state)", "protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter g$, net.minecraft.core.BlockPos p$)")
        replace("b.getContent()", "b.content")
        replace("bucket.getContent()", "bucket.content")
        replace(".typeHolder()", ".getItemHolder()")
        replace("level.isBrightOutside()", "level.isDay()")
        replace("ChunkPos.containing(", "new ChunkPos(")
        replace("player.getInventory().getSelectedSlot()", "player.getInventory().selected")
        replace("recipe.value().assemble(input)", "recipe.value().assemble(input, level.registryAccess())")
        replace("r.value().assemble(in)", "r.value().assemble(in, level.registryAccess())")
        replace(".value().cookingTime()", ".value().getCookingTime()")
        replace("_SOUND.location()", "_SOUND.getLocation()")
        replace(".getCraftingRemainder() != null", ".hasCraftingRemainingItem()")
        replace(", p.getYRot(), p.getXRot(), true)", ", p.getYRot(), p.getXRot())")
        replace("level.sendParticles(viewer, particle, false, false,", "level.sendParticles(viewer, particle, (boolean) false,")
        replace("crop.getCloneItemStack(level, p, false)", "crop.getBlock().getCloneItemStack(level, p, crop)")
        replace("((Shearable) mob).shear(level, SoundSource.BLOCKS, new ItemStack(Items.SHEARS))", "((Shearable) mob).shear(SoundSource.BLOCKS)")
        replace("level.recipeAccess()", "level.getRecipeManager()")
        replace(".getMinY()", ".getMinBuildHeight()")
        replace("BuiltInRegistries.ITEM.getValue(", "BuiltInRegistries.ITEM.get(")
        replace("BuiltInRegistries.FLUID.getValue(", "BuiltInRegistries.FLUID.get(")
        replace("InteractionResult.TRY_WITH_EMPTY_HAND", "$mc.Interactions.TRY_WITH_EMPTY_HAND")
    }
}
