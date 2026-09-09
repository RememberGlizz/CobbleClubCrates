package com.cobbleclub.crates.client;

import com.cobbleclub.crates.block.entity.ModBlockEntities;
import com.cobbleclub.crates.block.ModBlocks;
import com.cobbleclub.crates.client.render.CrateBlockEntityRenderer;
import com.cobbleclub.crates.client.screen.CrateMenuScreen;
import com.cobbleclub.crates.client.screen.CrateOpeningScreen;
import com.cobbleclub.crates.client.screen.CratePreviewScreen;
import com.cobbleclub.crates.network.CrateMenuPayload;
import com.cobbleclub.crates.network.CratePreviewPayload;
import com.cobbleclub.crates.network.CrateResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public final class CobbleClubCratesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                ModBlocks.BASIC_CRATE, ModBlocks.VOTE_CRATE, ModBlocks.SHINY_CRATE, ModBlocks.LEGENDARY_CRATE);
        BlockEntityRendererFactories.register(ModBlockEntities.CRATE_BLOCK_ENTITY, CrateBlockEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(CrateMenuPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new CrateMenuScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(CratePreviewPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new CratePreviewScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(CrateResultPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new CrateOpeningScreen(payload))));
    }
}
