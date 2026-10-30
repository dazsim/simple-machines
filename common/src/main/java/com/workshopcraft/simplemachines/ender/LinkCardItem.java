package com.workshopcraft.simplemachines.ender;

import com.workshopcraft.simplemachines.SimpleMachines;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Right-click writes the card's code into a device, sneak right-click reads the device's code back. */
public class LinkCardItem extends Item {
    public LinkCardItem(Properties properties) {
        super(properties);
    }

    /** Returns null when the block is not an ender device or the stack is not a written card. */
    public static @Nullable InteractionResult writeTo(ItemStack stack, Level level, BlockPos pos, Player player) {
        final EnderLink card = stack.get(SimpleMachines.ENDER_LINK);
        if (card == null || player.isSecondaryUseActive() || !(level.getBlockEntity(pos) instanceof EnderLink.Holder holder)) return null;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final EnderLink old = holder.link();
        if (old != null && !old.ownedBy(player) || !EnderLink.allows(card, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.ender_not_owner").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        holder.setLink(old == null ? card : old.withCode(card.code(), card.owner(), card.priv()));
        level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.sendOverlayMessage(Component.translatable("message.simplemachines.ender_set", card.digits()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        if (player == null || !player.isSecondaryUseActive() || !(level.getBlockEntity(context.getClickedPos()) instanceof EnderLink.Holder holder)) return InteractionResult.PASS;
        final EnderLink link = holder.link();
        if (link == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!EnderLink.allows(link, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.ender_not_owner").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        context.getItemInHand().set(SimpleMachines.ENDER_LINK, new EnderLink(link.code(), link.owner(), link.priv(), ""));
        player.sendOverlayMessage(Component.translatable("message.simplemachines.ender_copied", link.digits()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final EnderLink link = stack.get(SimpleMachines.ENDER_LINK);
        if (link != null) link.tooltip(out);
        else out.accept(Component.translatable("tooltip.simplemachines.link_card_blank").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.link_card").withStyle(ChatFormatting.DARK_GRAY));
    }
}
