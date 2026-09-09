package com.cobbleclub.crates.block.entity;

import com.cobbleclub.crates.CobbleClubCrates;
import com.cobbleclub.crates.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
    public static final BlockEntityType<CrateBlockEntity> CRATE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            CobbleClubCrates.id("crate"),
            FabricBlockEntityTypeBuilder.create(
                    CrateBlockEntity::new,
                    ModBlocks.BASIC_CRATE, ModBlocks.VOTE_CRATE,
                    ModBlocks.SHINY_CRATE, ModBlocks.LEGENDARY_CRATE
            ).build()
    );

    private ModBlockEntities() {}

    public static void register() {
    }
}
