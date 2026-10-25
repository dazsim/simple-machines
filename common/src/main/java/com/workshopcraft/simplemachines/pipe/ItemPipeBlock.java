package com.workshopcraft.simplemachines.pipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import com.workshopcraft.simplemachines.cable.ConduitBlockEntity;
import com.workshopcraft.simplemachines.cable.Conduits;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Item pipe. Ends next to an inventory deliver into it; the screwdriver cycles them to extract, then disconnected. */
public class ItemPipeBlock extends Block implements EntityBlock, Conduits.Recompute {
    public static final MapCodec<ItemPipeBlock> CODEC = simpleCodec(ItemPipeBlock::new);
    public static final int INTERVAL = 10;
    public static final int BATCH = 8;
    public static final Map<Direction, EnumProperty<Link>> LINKS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (Direction d : Direction.values()) LINKS.put(d, EnumProperty.create(d.getSerializedName(), Link.class));
        final VoxelShape core = Block.box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
        final VoxelShape[] arms = {
                Block.box(6.5, 0, 6.5, 9.5, 6, 9.5), Block.box(6.5, 10, 6.5, 9.5, 16, 9.5),
                Block.box(6.5, 6.5, 0, 9.5, 9.5, 6), Block.box(6.5, 6.5, 10, 9.5, 9.5, 16),
                Block.box(0, 6.5, 6.5, 6, 9.5, 9.5), Block.box(10, 6.5, 6.5, 16, 9.5, 9.5)};
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = core;
            for (int i = 0; i < 6; i++) if ((mask & 1 << i) != 0) shape = Shapes.or(shape, arms[i]);
            SHAPES[mask] = shape;
        }
    }

    public ItemPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (EnumProperty<Link> p : LINKS.values()) state = state.setValue(p, Link.NONE);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<ItemPipeBlock> codec() {
        return CODEC;
    }

    public static Link link(BlockState state, Direction d) {
        return state.getBlock() instanceof ItemPipeBlock ? state.getValue(LINKS.get(d)) : Link.NONE;
    }

    private static Link connects(LevelReader level, BlockPos pos, Direction dir, BlockState neighbour, Link current) {
        if (neighbour.getBlock() instanceof ItemPipeBlock) return ConduitBlockEntity.joins(level, pos, dir) ? Link.PIPE : Link.NONE;
        if (neighbour.isAir()) return Link.NONE;
        if (!(level instanceof Level real)) return current;
        if (!Transfer.lookup.has(real, pos.relative(dir), dir.getOpposite())) return Link.NONE;
        return current.end() ? current : Link.LINK;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction d : Direction.values()) {
            state = state.setValue(LINKS.get(d), connects(level, pos, d, level.getBlockState(pos.relative(d)), Link.NONE));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        final EnumProperty<Link> p = LINKS.get(dir);
        final BlockState next = state.setValue(p, connects(level, pos, dir, neighbour, state.getValue(p)));
        if (next != state && level instanceof ServerLevel server) PipeNetwork.invalidate(server);
        return next;
    }

    @Override
    public BlockState recompute(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            final EnumProperty<Link> p = LINKS.get(d);
            state = state.setValue(p, connects(level, pos, d, level.getBlockState(pos.relative(d)), state.getValue(p)));
        }
        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConduitBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return Conduits.dye(held, state, level, pos, player);
    }

    public static boolean extracts(BlockState state) {
        for (EnumProperty<Link> p : LINKS.values()) if (state.getValue(p) == Link.EXTRACT) return true;
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!(level instanceof ServerLevel server)) return;
        if (!oldState.is(this)) PipeNetwork.invalidate(server);
        if (extracts(state) && !level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!extracts(state)) return;
        final PipeNetwork net = PipeNetwork.of(level, pos);
        for (Direction d : Direction.values()) {
            if (state.getValue(LINKS.get(d)) != Link.EXTRACT) continue;
            net.pull(level, pos.relative(d), d.getOpposite());
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        PipeNetwork.invalidate(level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        final Direction[] dirs = Direction.values();
        for (int i = 0; i < 6; i++) if (state.getValue(LINKS.get(dirs[i])).drawn()) mask |= 1 << i;
        return SHAPES[mask];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction d : Direction.values()) builder.add(LINKS.get(d));
    }
}
