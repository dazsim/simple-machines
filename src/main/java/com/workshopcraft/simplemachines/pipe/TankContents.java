package com.workshopcraft.simplemachines.pipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

/** What a tank held, kept on the item when the tank is broken. Amount in droplets (81 per mB). */
public record TankContents(Fluid fluid, long amount) {
    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(TankContents::fluid),
            Codec.LONG.fieldOf("amount").forGetter(TankContents::amount)).apply(i, TankContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TankContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), TankContents::fluid,
            ByteBufCodecs.VAR_LONG.<RegistryFriendlyByteBuf>cast(), TankContents::amount, TankContents::new);
}
