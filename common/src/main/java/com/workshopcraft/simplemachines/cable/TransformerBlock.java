package com.workshopcraft.simplemachines.cable;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Steps power between two tiers. The front, facing whoever placed it, is the high side; the other five faces are the low side. A screwdriver moves the front. */
public class TransformerBlock extends Block implements EntityBlock {
    public static final MapCodec<TransformerBlock> CODEC = simpleCodec(p -> new TransformerBlock(p, Voltage.MV));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public final int high;

    public TransformerBlock(Properties properties, int high) {
        super(properties);
        this.high = high;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TransformerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
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
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransformerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != SimpleMachines.transformerEntity) return null;
        return (l, pos, s, be) -> ((TransformerBlockEntity) be).serverTick(server, pos, s);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof TransformerBlockEntity be) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.transformer", Voltage.NAME[high],
                    Voltage.NAME[high - 1], be.up.energy, be.down.energy, be.up.capacity));
        }
        return InteractionResult.SUCCESS;
    }

    /** Screwdriver: the clicked face becomes the high side; sneaking swaps the high side to the face opposite. */
    public void screwdriver(ServerLevel level, BlockPos pos, BlockState state, Player player, Direction face, boolean sneak) {
        final Direction front = sneak ? state.getValue(FACING).getOpposite() : face;
        if (front != state.getValue(FACING)) {
            level.setBlock(pos, state.setValue(FACING, front), 3);
            CableNetwork.invalidate(level);
        }
        player.sendOverlayMessage(Component.translatable("message.simplemachines.transformer_side", Voltage.NAME[high],
                Component.translatable("direction.simplemachines." + front.getSerializedName()), Voltage.NAME[high - 1]));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CableNetwork.invalidate(level);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server && !oldState.is(this)) CableNetwork.invalidate(server);
    }
}
