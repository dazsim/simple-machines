package com.workshopcraft.simplemachines.entangled;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import com.workshopcraft.simplemachines.machine.ChargerBlockEntity;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
//? if >=26.1 {
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
//?}
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=26.1
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Opens the Entangled Buffer channel on its code from anywhere, 1,000 FE a time from its own battery. Held, or worn and opened with a key. */
//? if >=26.1 {
public class EntangledBackpackItem extends Item {
//?} else {
/*public class EntangledBackpackItem extends Item implements net.minecraft.world.item.Equipable {
*///?}
    public static final int CAPACITY = 50_000, PER_OPEN = 1000;
    public static final String OPEN_KEY = "key.simplemachines.open_backpack";
    public static final Identifier OPEN_ID = SimpleMachines.id("open_backpack");
    //? if >=26.1 {
    @SuppressWarnings("unchecked")
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create((ResourceKey<? extends Registry<EquipmentAsset>>) EquipmentAssets.ROOT_ID,
            SimpleMachines.id("entangled_backpack"));
    //?} else {
    /*
    *///?}

    /** Sent by the open key, opens the worn backpack. */
    public record OpenPayload() implements CustomPacketPayload {
        public static final OpenPayload INSTANCE = new OpenPayload();
        public static final CustomPacketPayload.Type<OpenPayload> TYPE = new CustomPacketPayload.Type<>(OPEN_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<OpenPayload> type() {
            return TYPE;
        }
    }

    //? if >=26.1 {
    public static Equippable equippable() {
        return Equippable.builder(EquipmentSlot.CHEST).setAsset(ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setSwappable(false).build();
    }
    //?} else {
    /*@Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }
    *///?}

    public EntangledBackpackItem(Properties properties) {
        super(properties);
    }

    /** Backpacks never go inside a buffer. */
    public static boolean fitsBuffer(ItemStack stack) {
        return !(stack.getItem() instanceof EntangledBackpackItem);
    }

    public static int energy(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(SimpleMachines.ENERGY, 0), 0, CAPACITY);
    }

    @Override
    //? if >=26.1 {
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        return open(sp, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    //?} else {
    /*public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
    *///?}

    /** The open key: the backpack in the chest slot. */
    public static void openWorn(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof EntangledBackpackItem) open(player, EquipmentSlot.CHEST);
        else player.sendOverlayMessage(Component.translatable("message.simplemachines.backpack_not_worn").withStyle(ChatFormatting.RED));
    }

    private static boolean open(ServerPlayer player, EquipmentSlot from) {
        final ItemStack stack = player.getItemBySlot(from);
        if (player.containerMenu instanceof EntangledBufferMenu) return false;
        final EntangledLink link = stack.get(SimpleMachines.ENTANGLED_LINK);
        final String refused = link == null ? "backpack_no_code" : !EntangledLink.allows(link, player) ? "entangled_not_owner"
                : energy(stack) < PER_OPEN ? "backpack_no_power" : null;
        if (refused != null) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines." + refused, PowerTooltip.num(PER_OPEN), PowerTooltip.num(energy(stack)))
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        stack.set(SimpleMachines.ENERGY, energy(stack) - PER_OPEN);
        final EntangledData data = EntangledData.get(player.level().getServer());
        final View view = new View(data, data.channel(EntangledData.BUFFER, link).items, stack, from);
        final int slot = from == EquipmentSlot.MAINHAND ? player.getInventory().getSelectedSlot() : from == EquipmentSlot.OFFHAND ? Inventory.SLOT_OFFHAND : -1;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new EntangledBufferMenu(id, inv, view, new Data(stack, link), slot),
                Component.translatable("container.simplemachines.entangled_backpack")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5F, 1.2F);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * energy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return energy(stack) >= PER_OPEN ? 0x4FB8F0 : 0xC0392B;
    }

    @Override
    //? if >=26.1 {
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        final Consumer<Component> out = lines::add;
    *///?}
        final EntangledLink link = stack.get(SimpleMachines.ENTANGLED_LINK);
        if (link != null) link.tooltip(out);
        else out.accept(Component.translatable("tooltip.simplemachines.backpack_blank").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.charge", PowerTooltip.num(energy(stack)), PowerTooltip.num(CAPACITY)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.charges_at", PowerTooltip.tier(Voltage.LV), PowerTooltip.num(ChargerBlockEntity.RATE)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.uses_open", PowerTooltip.num(PER_OPEN)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.backpack").withStyle(ChatFormatting.DARK_GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.backpack_worn", Component.keybind(OPEN_KEY)).withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The channel's slots, valid while the same backpack stays in the hand or chest slot that opened it. */
    private record View(EntangledData data, NonNullList<ItemStack> items, ItemStack backpack, EquipmentSlot from) implements Container {
        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : items) if (!s.isEmpty()) return false;
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            final ItemStack s = ContainerHelper.removeItem(items, slot, count);
            if (!s.isEmpty()) setChanged();
            return s;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            stack.limitSize(getMaxStackSize(stack));
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return fitsBuffer(stack);
        }

        @Override
        public void setChanged() {
            data.setDirty();
        }

        @Override
        public boolean stillValid(Player player) {
            return player.getItemBySlot(from) == backpack && !backpack.isEmpty();
        }

        @Override
        public void clearContent() {
            items.clear();
        }
    }

    /** The backpack's battery in place of a block's, and the channel always online. */
    private record Data(ItemStack backpack, EntangledLink link) implements ContainerData {
        @Override
        public int get(int id) {
            final int e = energy(backpack);
            return switch (id) {
                case 2 -> e & 0xFFFF;
                case 3 -> e >>> 16;
                case 4 -> CAPACITY / 1000;
                case 0, 1, 5, 6 -> 0;
                default -> EntangledDeviceMenu.encode(id - EnergyMenu.DATA_COUNT, link, true, 0, 0);
            };
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return EntangledDeviceBlockEntity.DATA_COUNT;
        }
    }
}
