package com.workshopcraft.simplemachines.cable;

import com.workshopcraft.simplemachines.machine.WorkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Voltage tiers. Feeding a higher tier into a block of this mod rated lower blows that block up. */
public final class Voltage {
    public static final int LV = 0, MV = 1, HV = 2;
    public static final int[] RATE = {1000, 8000, 64000};
    public static final String[] NAME = {"LV", "MV", "HV"};
    private static final float POWER = 2.0F;

    private Voltage() {
    }

    /** Tier the block at pos accepts on side, or -1 when it is not one of ours or takes no energy there. */
    public static int rating(Level level, BlockPos pos, @Nullable Direction side) {
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock cable) return cable.tier;
        if (state.getBlock() instanceof TransformerBlock t) return side == state.getValue(BlockStateProperties.FACING) ? t.high : t.high - 1;
        if (level.getBlockEntity(pos) instanceof com.workshopcraft.simplemachines.quarry.QuarryBlockEntity) return MV;
        if (level.getBlockEntity(pos) instanceof WorkBlockEntity be && be.energyIn(side)) return LV;
        return -1;
    }

    /** Tier the block at pos sends towards the neighbour on side; anything not ours counts as LV. */
    public static int sending(Level level, BlockPos pos, Direction side) {
        final BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock cable) return cable.tier;
        if (state.getBlock() instanceof TransformerBlock t) return side == state.getValue(BlockStateProperties.FACING) ? t.high : t.high - 1;
        if (level.getBlockEntity(pos) instanceof com.workshopcraft.simplemachines.gen.SolarPanelBlockEntity solar) {
            return com.workshopcraft.simplemachines.gen.SolarPanelBlockEntity.VOLTAGE[solar.tier - 1];
        }
        return LV;
    }

    public static boolean overloads(Level level, BlockPos pos, @Nullable Direction side, int tier) {
        final int r = rating(level, pos, side);
        return r >= 0 && r < tier;
    }

    public static void blow(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) return;
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, POWER,
                terrainDamage(level.getServer()) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** config/simplemachines-server.properties, overvoltage_terrain_damage (default true). Read on each blast so edits apply live. */
    public static boolean terrainDamage(MinecraftServer server) {
        final Path file = server.getServerDirectory().resolve("config").resolve("simplemachines-server.properties");
        final Properties props = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    props.load(r);
                }
            }
            if (!props.containsKey("overvoltage_terrain_damage")) {
                props.setProperty("overvoltage_terrain_damage", "true");
                Files.createDirectories(file.getParent());
                try (Writer w = Files.newBufferedWriter(file)) {
                    props.store(w, "Simple Machines server settings");
                }
            }
        } catch (IOException e) {
            return true;
        }
        return !"false".equalsIgnoreCase(props.getProperty("overvoltage_terrain_damage", "true").trim());
    }
}
