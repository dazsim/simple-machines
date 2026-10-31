package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.item.PowerTooltip;
import com.workshopcraft.simplemachines.machine.ChargerBlockEntity;
import com.workshopcraft.simplemachines.machine.EnergyMenu;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Opens the Entangled Buffer channel on its code from anywhere, 1,000 FE a time from its own battery. */
public class EntangledBackpackItem extends Item {
    public static final int CAPACITY = 50_000, PER_OPEN = 1000;

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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        final EnderLink link = stack.get(SimpleMachines.ENDER_LINK);
        final String refused = link == null ? "backpack_no_code" : !EnderLink.allows(link, player) ? "ender_not_owner"
                : energy(stack) < PER_OPEN ? "backpack_no_power" : null;
        if (refused != null) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines." + refused).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        stack.set(SimpleMachines.ENERGY, energy(stack) - PER_OPEN);
        final EnderData data = EnderData.get(sp.level().getServer());
        final View view = new View(data, data.channel(EnderData.BUFFER, link).items, stack, hand);
        final int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new EntangledBufferMenu(id, inv, view, new Data(stack, link), slot),
                Component.translatable("container.simplemachines.entangled_backpack")));
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_CHEST_OPEN, SoundSource.PLAYERS, 0.5F, 1.2F);
        return InteractionResult.SUCCESS;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final EnderLink link = stack.get(SimpleMachines.ENDER_LINK);
        if (link != null) link.tooltip(out);
        else out.accept(Component.translatable("tooltip.simplemachines.backpack_blank").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.charge", PowerTooltip.num(energy(stack)), PowerTooltip.num(CAPACITY)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.charges_at", PowerTooltip.tier(Voltage.LV), PowerTooltip.num(ChargerBlockEntity.RATE)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.uses_open", PowerTooltip.num(PER_OPEN)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.backpack").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The channel's slots, valid while the same backpack stays in the hand that opened it. */
    private record View(EnderData data, NonNullList<ItemStack> items, ItemStack backpack, InteractionHand hand) implements Container {
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
            return player.getItemInHand(hand) == backpack && !backpack.isEmpty();
        }

        @Override
        public void clearContent() {
            items.clear();
        }
    }

    /** The backpack's battery in place of a block's, and the channel always online. */
    private record Data(ItemStack backpack, EnderLink link) implements ContainerData {
        @Override
        public int get(int id) {
            final int e = energy(backpack);
            return switch (id) {
                case 2 -> e & 0xFFFF;
                case 3 -> e >>> 16;
                case 4 -> CAPACITY / 1000;
                case 0, 1, 5, 6 -> 0;
                default -> EnderDeviceMenu.encode(id - EnergyMenu.DATA_COUNT, link, true, 0, 0);
            };
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return EnderDeviceBlockEntity.DATA_COUNT;
        }
    }
}
