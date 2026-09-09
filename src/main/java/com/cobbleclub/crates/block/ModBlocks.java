package com.cobbleclub.crates.block;

import com.cobbleclub.crates.CobbleClubCrates;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.Locale;

public final class ModBlocks {
    public static final Block BASIC_CRATE = register("basic_crate");
    public static final Block VOTE_CRATE = register("vote_crate");
    public static final Block SHINY_CRATE = register("shiny_crate");
    public static final Block LEGENDARY_CRATE = register("legendary_crate");

    private ModBlocks() {}

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.OPERATOR).register(entries -> {
            entries.add(BASIC_CRATE);
            entries.add(VOTE_CRATE);
            entries.add(SHINY_CRATE);
            entries.add(LEGENDARY_CRATE);
        });
    }

    public static Block byStyle(String style) {
        return switch (style == null ? "BASIC" : style.toUpperCase(Locale.ROOT)) {
            case "VOTE" -> VOTE_CRATE;
            case "SHINY" -> SHINY_CRATE;
            case "LEGENDARY" -> LEGENDARY_CRATE;
            default -> BASIC_CRATE;
        };
    }

    private static Block register(String name) {
        Block block = new CrateBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                .strength(-1.0F, 3_600_000.0F).luminance(state -> 7).nonOpaque());
        Registry.register(Registries.BLOCK, CobbleClubCrates.id(name), block);
        Registry.register(Registries.ITEM, CobbleClubCrates.id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }
}
