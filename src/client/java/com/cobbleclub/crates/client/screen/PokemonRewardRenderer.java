package com.cobbleclub.crates.client.screen;

import com.cobbleclub.crates.network.RewardView;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.gui.ProfileTransformType;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

final class PokemonRewardRenderer {
    private final Map<String, PokemonDisplay> pokemonCache = new HashMap<>();

    void draw(DrawContext context, RewardView reward, int x, int y, int size, float delta) {
        if ("POKEMON".equalsIgnoreCase(reward.type())) {
            drawPokemon(context, reward.pokemon(), x, y, size, delta);
            return;
        }

        Identifier itemId = Identifier.tryParse(reward.item());
        if (itemId == null || !Registries.ITEM.containsId(itemId)) return;
        ItemStack stack = new ItemStack(Registries.ITEM.get(itemId), Math.max(1, reward.count()));
        context.getMatrices().push();
        float scale = Math.max(1.0F, size / 22.0F);
        context.getMatrices().translate(x + (size - 16F * scale) / 2F, y + (size - 16F * scale) / 2F, 100F);
        context.getMatrices().scale(scale, scale, 1F);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    private void drawPokemon(DrawContext context, String properties, int x, int y, int size, float delta) {
        PokemonDisplay display = pokemonCache.computeIfAbsent(properties == null ? "" : properties, this::create);
        if (display == null) return;

        context.enableScissor(x, y, x + size, y + size);
        context.getMatrices().push();
        context.getMatrices().translate(x + size / 2.0, y + size * 0.12, 120.0);
        float outerScale = size / 32.0F;
        float modelScale = size >= 80 ? 5.5F : 11.0F;
        context.getMatrices().scale(outerScale, outerScale, 1F);
        PokemonGuiUtilsKt.drawProfilePokemon(
                display.pokemon(), context.getMatrices(),
                new Quaternionf().rotationXYZ(
                        (float) Math.toRadians(13D),
                        (float) Math.toRadians(35D),
                        0F
                ),
                PoseType.PROFILE, display.state(), delta, modelScale,
                ProfileTransformType.PROFILE, false, 1F, 1F, 1F, 1F, 0F, 0F, 13
        );
        context.getMatrices().pop();
        context.disableScissor();
    }

    private PokemonDisplay create(String properties) {
        try {
            if (properties.isBlank()) return null;
            RenderablePokemon pokemon = PokemonProperties.Companion.parse(properties).asRenderablePokemon();
            return new PokemonDisplay(pokemon, new FloatingState());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record PokemonDisplay(RenderablePokemon pokemon, FloatingState state) {}
}
