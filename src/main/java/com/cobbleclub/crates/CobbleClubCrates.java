package com.cobbleclub.crates;

import com.cobbleclub.crates.block.ModBlocks;
import com.cobbleclub.crates.block.entity.ModBlockEntities;
import com.cobbleclub.crates.command.CrateCommands;
import com.cobbleclub.crates.config.CrateConfigManager;
import com.cobbleclub.crates.network.CrateNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobbleClubCrates implements ModInitializer {
    public static final String MOD_ID = "cobbleclub_crates";
    public static final Logger LOGGER = LoggerFactory.getLogger("CobbleClub Crates");

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModBlockEntities.register();
        CrateNetworking.registerPayloads();
        CrateNetworking.registerServerReceivers();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CrateCommands.register(dispatcher, registryAccess));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> CrateConfigManager.load());
        LOGGER.info("CobbleClub Crates initialized");
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
