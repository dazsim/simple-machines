package com.workshopcraft.simplemachines.ender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.pipe.TankContents;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Every ender channel on the server, keyed by kind, scope and code. Contents stay while no device is placed. */
public class EnderData extends SavedData {
    public static final String BUFFER = "buffer", TANK = "tank", RELAY = "relay", PAD = "pad";
    public static final int SLOTS = 27, MAX_PADS = 2;

    public static final Codec<EnderData> CODEC = Codec.unboundedMap(Codec.STRING, Channel.CODEC).xmap(EnderData::new, d -> {
        final Map<String, Channel> kept = new HashMap<>();
        d.channels.forEach((k, c) -> { if (!c.isEmpty()) kept.put(k, c); });
        return kept;
    });
    public static final SavedDataType<EnderData> TYPE = new SavedDataType<>(SimpleMachines.id("ender"), EnderData::new, CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Channel> channels;

    public EnderData() {
        this(new HashMap<>());
    }

    private EnderData(Map<String, Channel> map) {
        channels = new HashMap<>(map);
    }

    public static EnderData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static String key(String kind, EnderLink link) {
        return key(kind, link.scope(), link.code());
    }

    private static String key(String kind, String scope, int code) {
        return kind + "/" + scope + "/" + EnderLink.format(code);
    }

    public Channel channel(String kind, EnderLink link) {
        return channels.computeIfAbsent(key(kind, link), k -> {
            setDirty();
            return new Channel(link.owner(), link.priv());
        });
    }

    public @Nullable Channel find(String kind, EnderLink link) {
        return channels.get(key(kind, link));
    }

    /** Whether any channel of any kind already uses this code in the scope. */
    public boolean used(String scope, int code) {
        for (String kind : List.of(BUFFER, TANK, RELAY, PAD)) {
            final Channel c = channels.get(key(kind, scope, code));
            if (c != null && !c.isEmpty()) return true;
        }
        return false;
    }

    public int freeCode(String scope, RandomSource random) {
        int code = random.nextInt(EnderLink.MAX + 1);
        for (int i = 0; i < 64 && used(scope, code); i++) code = random.nextInt(EnderLink.MAX + 1);
        return code;
    }

    public int size() {
        return channels.size();
    }

    public static class Channel {
        public static final Codec<Channel> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(c -> c.owner),
                Codec.BOOL.optionalFieldOf("private", false).forGetter(c -> c.priv),
                ItemContainerContents.CODEC.optionalFieldOf("items", ItemContainerContents.EMPTY).forGetter(c -> ItemContainerContents.fromItems(c.items)),
                TankContents.CODEC.optionalFieldOf("fluid").forGetter(c -> Optional.ofNullable(c.fluid)),
                GlobalPos.CODEC.listOf().optionalFieldOf("pads", List.of()).forGetter(c -> c.pads),
                GlobalPos.CODEC.listOf().optionalFieldOf("loaders", List.of()).forGetter(c -> c.loaders)
        ).apply(i, Channel::new));

        public final UUID owner;
        public boolean priv;
        public final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        public @Nullable TankContents fluid;
        public final List<GlobalPos> pads = new ArrayList<>();
        public final List<GlobalPos> loaders = new ArrayList<>();

        Channel(UUID owner, boolean priv) {
            this.owner = owner;
            this.priv = priv;
        }

        private Channel(UUID owner, boolean priv, ItemContainerContents items, Optional<TankContents> fluid, List<GlobalPos> pads, List<GlobalPos> loaders) {
            this(owner, priv);
            items.copyInto(this.items);
            this.fluid = fluid.orElse(null);
            this.pads.addAll(pads.subList(0, Math.min(MAX_PADS, pads.size())));
            this.loaders.addAll(loaders);
        }

        public boolean isEmpty() {
            if (fluid != null && fluid.amount() > 0) return false;
            if (!pads.isEmpty() || !loaders.isEmpty()) return false;
            for (ItemStack s : items) if (!s.isEmpty()) return false;
            return true;
        }
    }
}
