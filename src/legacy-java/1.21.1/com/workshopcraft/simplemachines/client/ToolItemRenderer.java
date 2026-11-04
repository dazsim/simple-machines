package com.workshopcraft.simplemachines.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.simplemachines.SimpleMachines;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the powered tools by walking the 26.x item model definition (copied to assets/simplemachines/tool_models) over the stack's parts. */
public class ToolItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final String[] TOOLS = {"chainsaw", "electric_drill"};
    public static Function<ResourceLocation, BakedModel> models;
    private static ToolItemRenderer instance;
    private static final Map<String, JsonObject> TREES = new HashMap<>();
    static ItemStack lastStack = ItemStack.EMPTY;
    static LivingEntity lastEntity;

    public ToolItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static ToolItemRenderer get() {
        if (instance == null) instance = new ToolItemRenderer();
        return instance;
    }

    /** Item property simplemachines:tool_holder, only there to learn which entity holds the stack about to be drawn. */
    public static final net.minecraft.client.renderer.item.ClampedItemPropertyFunction HOLDER = (stack, level, entity, seed) -> {
        lastStack = stack;
        lastEntity = entity;
        return 0;
    };

    private static JsonObject tree(ResourceManager rm, String name) {
        return TREES.computeIfAbsent(name, n -> {
            try (Reader r = rm.openAsReader(SimpleMachines.id("tool_models/" + n + ".json"))) {
                return JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("model");
            } catch (Exception e) {
                return new JsonObject();
            }
        });
    }

    public static List<ResourceLocation> leafModels(ResourceManager rm) {
        TREES.clear();
        final Set<ResourceLocation> out = new LinkedHashSet<>();
        for (String t : TOOLS) collect(tree(rm, t), out);
        return new ArrayList<>(out);
    }

    private static void collect(JsonElement e, Set<ResourceLocation> out) {
        if (e == null || !e.isJsonObject()) return;
        final JsonObject o = e.getAsJsonObject();
        if (o.has("model") && o.get("model").isJsonPrimitive()) out.add(ResourceLocation.parse(o.get("model").getAsString()));
        if (o.has("models")) o.getAsJsonArray("models").forEach(x -> collect(x, out));
        if (o.has("cases")) o.getAsJsonArray("cases").forEach(x -> collect(x.getAsJsonObject().get("model"), out));
        if (o.has("entries")) o.getAsJsonArray("entries").forEach(x -> collect(x.getAsJsonObject().get("model"), out));
        for (String k : new String[]{"fallback", "on_true", "on_false"}) collect(o.get(k), out);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (models == null) return;
        final Minecraft mc = Minecraft.getInstance();
        final ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        final LivingEntity entity = lastStack == stack ? lastEntity : null;
        final List<ResourceLocation> parts = new ArrayList<>();
        pick(tree(mc.getResourceManager(), key.getPath()), stack, entity, parts);
        final ItemRenderer ir = mc.getItemRenderer();
        final var buffer = ItemRenderer.getFoilBufferDirect(buffers, ItemBlockRenderTypes.getRenderType(stack, true), true, stack.hasFoil());
        for (ResourceLocation id : parts) {
            final BakedModel m = models.apply(id);
            if (m != null) ir.renderModelLists(m, stack, light, overlay, pose, buffer);
        }
    }

    private static void pick(JsonObject o, ItemStack stack, LivingEntity entity, List<ResourceLocation> out) {
        final String type = o.has("type") ? o.get("type").getAsString() : "minecraft:empty";
        switch (type) {
            case "minecraft:model" -> out.add(ResourceLocation.parse(o.get("model").getAsString()));
            case "minecraft:composite" -> o.getAsJsonArray("models").forEach(x -> pick(x.getAsJsonObject(), stack, entity, out));
            case "minecraft:condition" -> pick(o.getAsJsonObject(running(stack, entity) ? "on_true" : "on_false"), stack, entity, out);
            case "minecraft:range_dispatch" -> {
                final float v = value(o.get("property").getAsString(), stack, entity);
                JsonObject chosen = o.has("fallback") ? o.getAsJsonObject("fallback") : null;
                float best = Float.NEGATIVE_INFINITY;
                for (JsonElement e : o.getAsJsonArray("entries")) {
                    final float t = e.getAsJsonObject().get("threshold").getAsFloat();
                    if (t <= v && t >= best) {
                        best = t;
                        chosen = e.getAsJsonObject().getAsJsonObject("model");
                    }
                }
                if (chosen != null) pick(chosen, stack, entity, out);
            }
            case "minecraft:select" -> {
                final JsonElement v = component(o.get("component").getAsString(), stack);
                for (JsonElement c : o.getAsJsonArray("cases")) {
                    final JsonElement when = c.getAsJsonObject().get("when");
                    boolean hit = false;
                    if (v != null) {
                        if (when.isJsonArray()) for (JsonElement w : when.getAsJsonArray()) hit |= w.equals(v);
                        else hit = when.equals(v);
                    }
                    if (hit) {
                        pick(c.getAsJsonObject().getAsJsonObject("model"), stack, entity, out);
                        return;
                    }
                }
                if (o.has("fallback")) pick(o.getAsJsonObject("fallback"), stack, entity, out);
            }
            default -> { }
        }
    }

    private static boolean running(ItemStack stack, LivingEntity entity) {
        return ToolRunning.INSTANCE.unclampedCall(stack, null, entity, 0) > 0;
    }

    private static float value(String property, ItemStack stack, LivingEntity entity) {
        if (property.endsWith(":spin")) return ToolSpin.INSTANCE.unclampedCall(stack, null, entity, 0);
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static JsonElement component(String id, ItemStack stack) {
        final DataComponentType<Object> type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(id));
        if (type == null || type.codec() == null) return null;
        final Object v = stack.get(type);
        return v == null ? null : type.codec().encodeStart(JsonOps.INSTANCE, v).result().orElse(null);
    }
}
