package com.workshopcraft.simplemachines.entangled;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
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
public record EntangledLink(int code, UUID owner, boolean priv, String modules) {
    public static final int MAX = 999_999;
    public static final Codec<EntangledLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.intRange(0, MAX).fieldOf("code").forGetter(EntangledLink::code),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(EntangledLink::owner),
            Codec.BOOL.optionalFieldOf("private", false).forGetter(EntangledLink::priv),
            Codec.STRING.optionalFieldOf("modules", "").forGetter(EntangledLink::modules)
    ).apply(i, EntangledLink::new));
    public static final StreamCodec<ByteBuf, EntangledLink> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EntangledLink::code, UUIDUtil.STREAM_CODEC, EntangledLink::owner,
            ByteBufCodecs.BOOL, EntangledLink::priv, ByteBufCodecs.STRING_UTF8, EntangledLink::modules, EntangledLink::new);

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
    public static boolean allows(@Nullable EntangledLink link, Player player) {
        return link == null || !link.priv || link.ownedBy(player);
    }

    /** Whether the Entangled Programmer can write a code onto this stack. */
    public static boolean programmable(net.minecraft.world.item.ItemStack stack) {
        return stack.is(SimpleMachines.linkCard) || stack.is(SimpleMachines.entangledBackpack);
    }

    public EntangledLink withCode(int c, UUID who, boolean p) {
        return new EntangledLink(c, who, p, modules);
    }

    public EntangledLink withPrivate(boolean p) {
        return new EntangledLink(code, owner, p, modules);
    }

    public void tooltip(Consumer<Component> out) {
        out.accept(Component.translatable("tooltip.simplemachines.entangled_code", digits(),
                Component.translatable(priv ? "tooltip.simplemachines.entangled_private" : "tooltip.simplemachines.entangled_public"))
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    /** A block entity that sits on a channel. */
    public interface Holder {
        @Nullable EntangledLink link();

        void setLink(@Nullable EntangledLink link);
    }
}
