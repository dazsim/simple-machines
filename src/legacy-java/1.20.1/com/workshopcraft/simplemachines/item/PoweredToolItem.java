package com.workshopcraft.simplemachines.item;

import com.workshopcraft.simplemachines.SimpleMachines;
import com.workshopcraft.simplemachines.assembly.Upgrade;
import com.workshopcraft.simplemachines.cable.Voltage;
import com.workshopcraft.simplemachines.machine.ChargerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//import com.workshopcraft.simplemachines.compat.mc.item.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.text.NumberFormat;
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
public class PoweredToolItem extends net.minecraft.world.item.DiggerItem {
    public static final int CAPACITY = 50_000;
    public static final int RATE = 1000;
    public static final int PER_BLOCK = 50;
    public static final int PER_HIT = 50;
    public static final float SPEED = 8.5F;

    public final SoundEvent sound;

    // DiggerItem makes the enchanting table offer digger enchantments; 0 uses keeps the tool unbreakable
    private static final net.minecraft.world.item.Tier TIER = new net.minecraft.world.item.Tier() {
        public int getUses() { return 0; }
        public float getSpeed() { return SPEED; }
        public float getAttackDamageBonus() { return 0; }
        public int getLevel() { return 4; }
        public int getEnchantmentValue() { return 10; }
        public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() { return net.minecraft.world.item.crafting.Ingredient.EMPTY; }
    };

    public PoweredToolItem(Properties properties, SoundEvent sound, List<TagKey<Block>> mineable, float damage, float attackSpeed) {
        super(damage - 1, attackSpeed, TIER, mineable.get(0), tool(properties, mineable, damage, attackSpeed));
        this.sound = sound;
        this.mineable = List.copyOf(mineable);
        this.attributes = com.google.common.collect.ImmutableMultimap.<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", damage - 1, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", attackSpeed, AttributeModifier.Operation.ADDITION))
                .build();
    }

    private final List<TagKey<Block>> mineable;
    private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> attributes;

    private static Properties tool(Properties p, List<TagKey<Block>> mineable, float damage, float attackSpeed) {
        return p.stacksTo(1);
    }

    private boolean mines(BlockState state) {
        for (TagKey<Block> tag : mineable) if (state.is(tag)) return true;
        return false;
    }

    private float baseSpeed(BlockState state) {
        return mines(state) ? SPEED : 1.0F;
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
    }

    public static int energy(ItemStack stack) {
        return com.workshopcraft.simplemachines.compat.mc.Comps.getOrDefault(stack, SimpleMachines.ENERGY, 0);
    }

    public static boolean steel(ItemStack stack) {
        return "steel".equals(com.workshopcraft.simplemachines.compat.mc.Comps.get(stack, SimpleMachines.TOOL_FRAME));
    }

    public static List<Upgrade> upgrades(ItemStack stack) {
        return Upgrade.parse(com.workshopcraft.simplemachines.compat.mc.Comps.getOrDefault(stack, SimpleMachines.UPGRADES_FITTED, ""));
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
        return "diamond".equals(com.workshopcraft.simplemachines.compat.mc.Comps.get(stack, SimpleMachines.TOOL_TIP));
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
        return state.is(BlockTags.NEEDS_DIAMOND_TOOL);
    }

    /** The tool works on this block but its bit or chain is not hard enough, so it only throws white sparks. */
    public boolean refuses(ItemStack stack, BlockState state) {
        if (baseSpeed(state) <= 1.0F) return false;
        return diamond(stack) ? state.is(com.workshopcraft.simplemachines.compat.mc.Stacks.INCORRECT_FOR_NETHERITE_TOOL) : hard(state);
    }

    public static ItemStack charged(Item item) {
        final ItemStack stack = new ItemStack(item);
        com.workshopcraft.simplemachines.compat.mc.Comps.set(stack, SimpleMachines.ENERGY, CAPACITY);
        return stack;
    }

    public static boolean powered(ItemStack stack) {
        return energy(stack) >= perBlock(stack);
    }

    private static void use(ItemStack stack, int amount) {
        final int left = Math.max(0, energy(stack) - amount);
        if (left > 0) com.workshopcraft.simplemachines.compat.mc.Comps.set(stack, SimpleMachines.ENERGY, left);
        else com.workshopcraft.simplemachines.compat.mc.Comps.remove(stack, SimpleMachines.ENERGY);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!powered(stack)) return 1.0F;
        if (refuses(stack, state)) return 0.0F;
        final float speed = baseSpeed(state);
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

    public boolean allowNbtUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return oldStack.is(newStack.getItem());
    }

    @Override
    public boolean isCorrectToolForDrops(BlockState state) {
        return mines(state) && !state.is(com.workshopcraft.simplemachines.compat.mc.Stacks.INCORRECT_FOR_NETHERITE_TOOL);
    }

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return powered(stack) && !refuses(stack, state) && isCorrectToolForDrops(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && powered(stack)
                && baseSpeed(state) > 1.0F) {
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

    public void inventoryTick(ItemStack stack, Level world, net.minecraft.world.entity.Entity owner, int slot, boolean selected) {
        if (selected && world instanceof net.minecraft.server.level.ServerLevel level && owner instanceof net.minecraft.server.level.ServerPlayer player
                && player.getMainHandItem() == stack && level.getGameTime() % 2 == 0) ToolSparks.broadcast(this, stack, level, player);
    }

    public static boolean fells(ItemStack stack, BlockState state) {
        return stack.is(SimpleMachines.TOOLS.get(1)) && steel(stack) && diamond(stack) && state.is(BlockTags.LOGS);
    }

    @Override

    public boolean hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        use(stack, PER_HIT);
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
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

    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, java.util.List<Component> lines, TooltipFlag flag) {
        final Consumer<Component> out = lines::add;
        final NumberFormat f = NumberFormat.getIntegerInstance(Locale.ROOT);
        out.accept(Component.translatable("tooltip.simplemachines.charge", f.format(energy(stack)), f.format(capacity(stack))).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.charges_at", PowerTooltip.tier(Voltage.LV), f.format(ChargerBlockEntity.RATE)).withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.simplemachines.uses_block", f.format(perBlock(stack))).withStyle(ChatFormatting.GRAY));
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
