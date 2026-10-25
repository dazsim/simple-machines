package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Drill or chainsaw running on FE kept in the energy component; with too little charge it digs like a bare hand and drops nothing. Never breaks.
 * The bit or chain sets the tier: standard mines up to iron-tool blocks (diamond ore and below), diamond also mines diamond and
 * netherite level blocks (obsidian, ancient debris). An iron body runs those hard blocks at 40% speed, steel at full.
 * Steel body digs a quarter faster. Each Overclocker adds 50% speed and FE per block, each Energy Cell adds the base capacity
 * and 15% speed; the upgrade bonus is capped at +100%.
 * Steel chainsaw with a diamond-tipped chain fells whole natural trees, 50 FE per log.
 */
public class PoweredToolItem extends Item {
    public static final int CAPACITY = 50_000;
    public static final int RATE = 1000;
    public static final int PER_BLOCK = 50;
    public static final int PER_HIT = 50;
    public static final float SPEED = 8.5F;

    public final SoundEvent sound;

    public PoweredToolItem(Properties properties, SoundEvent sound, List<TagKey<Block>> mineable, float damage, float attackSpeed) {
        super(tool(properties, mineable, damage, attackSpeed));
        this.sound = sound;
    }

    private static Properties tool(Properties p, List<TagKey<Block>> mineable, float damage, float attackSpeed) {
        final HolderGetter<Block> blocks = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        final List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)));
        for (TagKey<Block> tag : mineable) rules.add(Tool.Rule.minesAndDrops(blocks.getOrThrow(tag), SPEED));
        return p.stacksTo(1).enchantable(10)
                .component(DataComponents.TOOL, new Tool(rules, 1.0F, 0, true))
                .component(DataComponents.SWING_ANIMATION, new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.NONE, 6))
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build());
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(SimpleMachines.ENERGY, 0);
    }

    public static boolean steel(ItemStack stack) {
        return "steel".equals(stack.get(SimpleMachines.TOOL_FRAME));
    }

    public static List<Upgrade> upgrades(ItemStack stack) {
        return Upgrade.parse(stack.getOrDefault(SimpleMachines.UPGRADES_FITTED, ""));
    }

    public static int capacity(ItemStack stack) {
        return CAPACITY * (1 + Upgrade.count(upgrades(stack), Upgrade.ENERGY_CELL));
    }

    public static int perBlock(ItemStack stack) {
        return PER_BLOCK * (2 + Upgrade.count(upgrades(stack), Upgrade.OVERCLOCKER)) / 2;
    }

    public static final float MAX_UPGRADE_BONUS = 1.0F;
    public static final float HARD_IRON_FACTOR = 0.4F;

    public static boolean diamond(ItemStack stack) {
        return "diamond".equals(stack.get(SimpleMachines.TOOL_TIP));
    }

    public static float upgradeBonus(ItemStack stack) {
        final List<Upgrade> ups = upgrades(stack);
        return Math.min(MAX_UPGRADE_BONUS, 0.5F * Upgrade.count(ups, Upgrade.OVERCLOCKER) + 0.15F * Upgrade.count(ups, Upgrade.ENERGY_CELL));
    }

    public static float speedFactor(ItemStack stack) {
        return (steel(stack) ? 1.25F : 1F) * (1F + upgradeBonus(stack));
    }

    /** Needs more than an iron tool: obsidian, ancient debris, netherite blocks. */
    public static boolean hard(BlockState state) {
        return state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
    }

    /** The tool works on this block but its bit or chain is not hard enough, so it only throws white sparks. */
    public boolean refuses(ItemStack stack, BlockState state) {
        if (super.getDestroySpeed(stack, state) <= 1.0F) return false;
        return diamond(stack) ? state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL) : hard(state);
    }

    public static ItemStack charged(Item item) {
        final ItemStack stack = new ItemStack(item);
        stack.set(SimpleMachines.ENERGY, CAPACITY);
        return stack;
    }

    public static boolean powered(ItemStack stack) {
        return energy(stack) >= perBlock(stack);
    }

    private static void use(ItemStack stack, int amount) {
        final int left = Math.max(0, energy(stack) - amount);
        if (left > 0) stack.set(SimpleMachines.ENERGY, left);
        else stack.remove(SimpleMachines.ENERGY);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!powered(stack)) return 1.0F;
        if (refuses(stack, state)) return 0.0F;
        final float speed = super.getDestroySpeed(stack, state);
        if (speed <= 1.0F) return speed;
        return speed * speedFactor(stack) * (hard(state) && !steel(stack) ? HARD_IRON_FACTOR : 1F);
    }

    // the charge changes every block; keep the tool steady in hand and keep digging (NeoForge and Fabric hooks)
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !oldStack.is(newStack.getItem());
    }

    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !oldStack.is(newStack.getItem());
    }

    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return oldStack.is(newStack.getItem());
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return powered(stack) && !refuses(stack, state) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && powered(stack)
                && super.getDestroySpeed(stack, state) > 1.0F) {
            use(stack, perBlock(stack));
            if (fells(stack, state) && owner instanceof net.minecraft.world.entity.player.Player player && !player.isShiftKeyDown()
                    && level instanceof net.minecraft.server.level.ServerLevel server) {
                for (BlockPos log : TreeFeller.logs(server, pos, state)) {
                    if (!powered(stack)) break;
                    if (server.destroyBlock(log, true, player)) use(stack, perBlock(stack));
                }
            }
        }
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity owner,
            @Nullable EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND && owner instanceof net.minecraft.server.level.ServerPlayer player
                && level.getGameTime() % 2 == 0) ToolSparks.broadcast(this, stack, level, player);
    }

    public static boolean fells(ItemStack stack, BlockState state) {
        return stack.is(SimpleMachines.TOOLS.get(1)) && steel(stack) && diamond(stack) && state.is(BlockTags.LOGS);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        use(stack, PER_HIT);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * energy(stack) / capacity(stack)), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return powered(stack) ? 0x4FB8F0 : 0xC0392B;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        final NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        out.accept(Component.translatable("tooltip.simplemachines.charge", f.format(energy(stack)), f.format(capacity(stack))).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable(steel(stack) ? "tooltip.simplemachines.steel_frame" : "tooltip.simplemachines.iron_frame").withStyle(ChatFormatting.GRAY));
        final boolean saw = stack.is(SimpleMachines.TOOLS.get(1));
        out.accept(Component.translatable(diamond(stack) ? (saw ? "tooltip.simplemachines.diamond_chain" : "tooltip.simplemachines.diamond_bit")
                : (saw ? "tooltip.simplemachines.standard_chain" : "tooltip.simplemachines.standard_bit")).withStyle(ChatFormatting.GRAY));
        if (diamond(stack) && !steel(stack)) out.accept(Component.translatable("tooltip.simplemachines.hard_slow").withStyle(ChatFormatting.DARK_GRAY));
        if (saw && steel(stack) && diamond(stack)) out.accept(Component.translatable("tooltip.simplemachines.fells_trees").withStyle(ChatFormatting.GOLD));
        final float bonus = upgradeBonus(stack);
        if (bonus > 0) out.accept(Component.translatable("tooltip.simplemachines.upgrade_speed", Math.round(bonus * 100)).withStyle(ChatFormatting.BLUE));
        final Component ups = Upgrade.summary(Upgrade.pack(upgrades(stack)), false);
        if (!ups.getString().isEmpty()) out.accept(Component.translatable("tooltip.simplemachines.upgrades", ups).withStyle(ChatFormatting.BLUE));
        if (!powered(stack)) out.accept(Component.translatable("tooltip.simplemachines.no_charge").withStyle(ChatFormatting.RED));
    }
}
