package com.cobbleclub.crates.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Optional runtime integration. No Pokeblocks classes are linked, so this mod still works without it. */
final class PokeblocksIntegration {
    private static final String MOD_ID = "pokeblocks";

    // Pokeblocks' own LEGENDARY rarity pool. Shiny versions remain in the Shiny crate.
    private static final Set<String> LEGENDARY_DOLLS = Set.of(
            "pokedoll_calyrex_animated",
            "pokedoll_venusaur",
            "pokedoll_blastoise",
            "pokedoll_gengar",
            "pokedoll_corviknight",
            "pokedoll_gholdengo",
            "pokedoll_netherite_gholdengo",
            "pokedoll_kyogre",
            "pokedoll_trevenant",
            "pokedoll_marshadow",
            "pokedoll_marshadow_zenith",
            "pokedoll_snorunt_family",
            "pokedoll_skibidi_mewlet"
    );

    private PokeblocksIntegration() {}

    static int inject(CrateDefinition crate) {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return 0;

        DollPool pool = DollPool.forCrate(crate.id);
        if (pool == null) return 0;

        Set<String> existingItems = new HashSet<>();
        Set<String> existingIds = new HashSet<>();
        for (RewardDefinition reward : crate.rewards) {
            existingItems.add(reward.item);
            existingIds.add(reward.id);
        }

        int added = 0;
        for (Identifier itemId : Registries.ITEM.getIds().stream()
                .filter(id -> MOD_ID.equals(id.getNamespace()))
                .sorted()
                .toList()) {
            String path = itemId.getPath();
            if (!pool.accepts(path)) continue;

            String fullId = itemId.toString();
            String rewardId = "pokeblocks_" + path;
            if (existingItems.contains(fullId) || existingIds.contains(rewardId)) continue;

            RewardDefinition reward = new RewardDefinition();
            reward.id = rewardId;
            reward.type = RewardType.ITEM.name();
            reward.displayName = displayName(path);
            reward.rarity = pool.rarity;
            reward.weight = pool.weight;
            reward.item = fullId;
            reward.count = 1;
            crate.rewards.add(reward.normalized(crate.rewards.size()));
            existingItems.add(fullId);
            existingIds.add(rewardId);
            added++;
        }
        return added;
    }

    private static String displayName(String path) {
        String name = path.substring("pokedoll_".length());
        boolean shiny = name.startsWith("shiny_");
        if (shiny) name = name.substring("shiny_".length());

        boolean animated = name.endsWith("_animated");
        if (animated) name = name.substring(0, name.length() - "_animated".length());
        boolean posed = name.endsWith("_posed");
        if (posed) name = name.substring(0, name.length() - "_posed".length());

        StringBuilder result = new StringBuilder();
        if (animated) result.append("Animated ");
        if (posed) result.append("Posing ");
        if (shiny) result.append("Shiny ");
        result.append(title(name)).append(" Pokédoll");
        return result.toString();
    }

    private static String title(String value) {
        StringBuilder result = new StringBuilder();
        for (String word : value.split("_")) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return result.toString();
    }

    private enum DollPool {
        VOTE("RARE", 0.75D) {
            @Override boolean accepts(String path) {
                return isDoll(path) && !isShiny(path) && !LEGENDARY_DOLLS.contains(path)
                        && !path.equals("pokedoll_washing_machine");
            }
        },
        SHINY("SHINY", 0.25D) {
            @Override boolean accepts(String path) {
                return isDoll(path) && isShiny(path) && !path.equals("pokedoll_shiny_pokemon_trophy");
            }
        },
        LEGENDARY("LEGENDARY", 0.35D) {
            @Override boolean accepts(String path) {
                return LEGENDARY_DOLLS.contains(path);
            }
        };

        private final String rarity;
        private final double weight;

        DollPool(String rarity, double weight) {
            this.rarity = rarity;
            this.weight = weight;
        }

        abstract boolean accepts(String path);

        static DollPool forCrate(String crateId) {
            if (crateId == null) return null;
            return switch (crateId.toLowerCase(Locale.ROOT)) {
                case "vote" -> VOTE;
                case "shiny" -> SHINY;
                case "legendary" -> LEGENDARY;
                default -> null;
            };
        }

        static boolean isDoll(String path) {
            return path.startsWith("pokedoll_");
        }

        static boolean isShiny(String path) {
            return path.startsWith("pokedoll_shiny_");
        }
    }
}
