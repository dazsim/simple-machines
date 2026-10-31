package com.workshopcraft.simplemachines.entangled;

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

    /** Returns null when the block is not an entangled device or the stack is not a written card. */
    public static @Nullable InteractionResult writeTo(ItemStack stack, Level level, BlockPos pos, Player player) {
        final EntangledLink card = stack.get(SimpleMachines.ENTANGLED_LINK);
        if (card == null || player.isSecondaryUseActive() || !(level.getBlockEntity(pos) instanceof EntangledLink.Holder holder)) return null;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        final EntangledLink old = holder.link();
        if (old != null && !old.ownedBy(player) || !EntangledLink.allows(card, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_not_owner").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        holder.setLink(old == null ? card : old.withCode(card.code(), card.owner(), card.priv()));
        level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_set", card.digits()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        if (player == null || !player.isSecondaryUseActive() || !(level.getBlockEntity(context.getClickedPos()) instanceof EntangledLink.Holder holder)) return InteractionResult.PASS;
        final EntangledLink link = holder.link();
        if (link == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!EntangledLink.allows(link, player)) {
            player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_not_owner").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        context.getItemInHand().set(SimpleMachines.ENTANGLED_LINK, new EntangledLink(link.code(), link.owner(), link.priv(), ""));
        player.sendOverlayMessage(Component.translatable("message.simplemachines.entangled_copied", link.digits()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final EntangledLink link = stack.get(SimpleMachines.ENTANGLED_LINK);
        if (link != null) link.tooltip(out);
        else out.accept(Component.translatable("tooltip.simplemachines.link_card_blank").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.link_card").withStyle(ChatFormatting.DARK_GRAY));
    }
}
