package com.cobbleclub.crates.network;

import com.cobbleclub.crates.service.CrateService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class CrateNetworking {
    private CrateNetworking() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(CrateMenuPayload.ID, CrateMenuPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CratePreviewPayload.ID, CratePreviewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CrateResultPayload.ID, CrateResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PreviewRequestPayload.ID, PreviewRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenCrateRequestPayload.ID, OpenCrateRequestPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(PreviewRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> CrateService.sendPreview(context.player(), payload.crateId())));
        ServerPlayNetworking.registerGlobalReceiver(OpenCrateRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> CrateService.open(context.player(), payload.crateId(), payload.payment())));
    }
}
