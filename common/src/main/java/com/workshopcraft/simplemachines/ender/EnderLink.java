package com.workshopcraft.simplemachines.ender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/** A frequency: 6 digit code, the player who set it, and whether only they may use it. Modules are the loader cards fitted, as ids. */
public record EnderLink(int code, UUID owner, boolean priv, String modules) {
    public static final int MAX = 999_999;
    public static final Codec<EnderLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.intRange(0, MAX).fieldOf("code").forGetter(EnderLink::code),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(EnderLink::owner),
            Codec.BOOL.optionalFieldOf("private", false).forGetter(EnderLink::priv),
            Codec.STRING.optionalFieldOf("modules", "").forGetter(EnderLink::modules)
    ).apply(i, EnderLink::new));
    public static final StreamCodec<ByteBuf, EnderLink> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnderLink::code, UUIDUtil.STREAM_CODEC, EnderLink::owner,
            ByteBufCodecs.BOOL, EnderLink::priv, ByteBufCodecs.STRING_UTF8, EnderLink::modules, EnderLink::new);

    public String scope() {
        return priv ? owner.toString() : "public";
    }

    public String digits() {
        return format(code);
    }

    public static String format(int code) {
        return String.format("%06d", code);
    }

    public boolean ownedBy(Player player) {
        return owner.equals(player.getUUID());
    }

    /** Whether player may open or recode a device on this link. */
    public static boolean allows(@Nullable EnderLink link, Player player) {
        return link == null || !link.priv || link.ownedBy(player);
    }

    public EnderLink withCode(int c, UUID who, boolean p) {
        return new EnderLink(c, who, p, modules);
    }

    public EnderLink withPrivate(boolean p) {
        return new EnderLink(code, owner, p, modules);
    }

    public void tooltip(Consumer<Component> out) {
        out.accept(Component.translatable("tooltip.simplemachines.ender_code", digits(),
                Component.translatable(priv ? "tooltip.simplemachines.ender_private" : "tooltip.simplemachines.ender_public"))
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    /** A block entity that sits on a channel. */
    public interface Holder {
        @Nullable EnderLink link();

        void setLink(@Nullable EnderLink link);
    }
}
