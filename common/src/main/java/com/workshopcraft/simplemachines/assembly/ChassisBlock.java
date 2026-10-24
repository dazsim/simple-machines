package com.workshopcraft.simplemachines.assembly;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Open frame that machines are built on. Steps: 0 empty, 1 assembly loose, 2 assembly fixed, 3 board loose,
 * 4 board fixed, 5 panels loose, 6 panels fixed, 7 worktop loose. Screwing the worktop in finishes the machine.
 */
public class ChassisBlock extends Block implements EntityBlock {
    public static final MapCodec<ChassisBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Tier.CODEC.fieldOf("tier").forGetter(b -> b.tier), propertiesCodec()).apply(i, ChassisBlock::new));
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Core> CORE = EnumProperty.create("core", Core.class);
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, 7);
    public static final EnumProperty<Finish> PANEL = EnumProperty.create("panel", Finish.class);

    public final Tier tier;

    public ChassisBlock(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CORE, Core.NONE)
                .setValue(STEP, 0).setValue(PANEL, Finish.NONE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChassisBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<ChassisBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Worktops.SCREWDRIVERS)) return InteractionResult.PASS;
        final InteractionResult result = Assembly.fit(state, level, pos, player, stack);
        return result != null ? result : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        final List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        drops.addAll(Assembly.parts(state, params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ChassisBlockEntity be ? be.worktop() : ""));
        return drops;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CORE, STEP, PANEL);
    }
}
