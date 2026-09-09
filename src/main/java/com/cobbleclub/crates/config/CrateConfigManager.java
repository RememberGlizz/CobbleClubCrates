package com.cobbleclub.crates.config;

import com.cobbleclub.crates.CobbleClubCrates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class CrateConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir()
            .resolve("cobbleclub-crates").resolve("crates");
    private static volatile Map<String, CrateDefinition> crates = Map.of();

    private CrateConfigManager() {}

    public static synchronized int load() {
        try {
            Files.createDirectories(DIRECTORY);
            try (Stream<Path> files = Files.list(DIRECTORY)) {
                if (files.noneMatch(path -> path.getFileName().toString().endsWith(".json"))) {
                    writeDefaults();
                }
            }

            Map<String, CrateDefinition> loaded = new LinkedHashMap<>();
            int pokeblocksRewards = 0;
            try (Stream<Path> files = Files.list(DIRECTORY)) {
                for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(Path::toString)).toList()) {
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        String fallback = path.getFileName().toString().replaceFirst("\\.json$", "");
                        CrateDefinition crate = GSON.fromJson(reader, CrateDefinition.class);
                        if (crate == null) throw new IllegalArgumentException("empty JSON document");
                        crate.normalized(fallback);
                        pokeblocksRewards += PokeblocksIntegration.inject(crate);
                        if (crate.rewards.isEmpty()) {
                            CobbleClubCrates.LOGGER.warn("Skipping crate {} because it has no rewards", path);
                            continue;
                        }
                        if (loaded.put(crate.id, crate) != null) {
                            CobbleClubCrates.LOGGER.warn("Duplicate crate id '{}' in {}; the later file won", crate.id, path);
                        }
                    } catch (Exception exception) {
                        CobbleClubCrates.LOGGER.error("Unable to load crate file {}", path, exception);
                    }
                }
            }
            crates = Map.copyOf(loaded);
            CobbleClubCrates.LOGGER.info("Loaded {} CobbleClub crates", crates.size());
            if (pokeblocksRewards > 0) {
                CobbleClubCrates.LOGGER.info("Added {} Pokeblocks doll rewards to the loaded crates", pokeblocksRewards);
            }
            return crates.size();
        } catch (IOException exception) {
            CobbleClubCrates.LOGGER.error("Unable to load CobbleClub crate configuration", exception);
            return 0;
        }
    }

    public static Optional<CrateDefinition> get(String id) {
        return Optional.ofNullable(crates.get(id == null ? "" : id.toLowerCase()));
    }

    public static List<CrateDefinition> all() {
        return crates.values().stream().filter(crate -> crate.enabled).toList();
    }

    public static Path directory() {
        return DIRECTORY;
    }

    private static void writeDefaults() throws IOException {
        writeAtomic(DIRECTORY.resolve("legendary.json"), legendaryCrate());
        writeAtomic(DIRECTORY.resolve("shiny.json"), shinyCrate());
        writeAtomic(DIRECTORY.resolve("vote.json"), voteCrate());
    }

    private static void writeAtomic(Path destination, CrateDefinition crate) throws IOException {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            GSON.toJson(crate, writer);
        }
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static CrateDefinition legendaryCrate() {
        CrateDefinition crate = base("legendary", "Legendary Crate", "LEGENDARY", "legendary_key", "Legendary Key", 2750L);
        String[] species = {
                "articuno", "zapdos", "moltres", "mewtwo", "mew", "raikou", "entei", "suicune",
                "lugia", "hooh", "celebi", "regirock", "regice", "registeel", "latias", "latios",
                "kyogre", "groudon", "rayquaza", "jirachi", "deoxys", "uxie", "mesprit", "azelf",
                "dialga", "palkia", "heatran", "regigigas", "giratina", "cresselia", "victini",
                "cobalion", "terrakion", "virizion", "tornadus", "thundurus", "reshiram", "zekrom",
                "landorus", "kyurem", "xerneas", "yveltal", "zygarde", "diancie", "hoopa", "volcanion",
                "typenull", "silvally", "tapukoko", "tapulele", "tapubulu", "tapufini", "cosmog",
                "solgaleo", "lunala", "necrozma", "zacian", "zamazenta", "eternatus"
        };
        for (String name : species) {
            crate.rewards.add(pokemon(name, title(name), "LEGENDARY", 1.0D, name + " level=70"));
        }
        crate.rewards.add(gems("gem_jackpot", "1,000 Gems", "EPIC", 4.0D, 1000L));
        crate.rewards.add(item("master_balls", "3 Master Balls", "EPIC", 3.0D, "cobblemon:master_ball", 3));
        return crate.normalized(crate.id);
    }

    private static CrateDefinition shinyCrate() {
        CrateDefinition crate = base("shiny", "Shiny Pokémon Crate", "SHINY", "shiny_key", "Shiny Key", 1250L);
        String[] species = {
                "charizard", "blastoise", "venusaur", "gengar", "dragonite", "tyranitar", "gardevoir",
                "metagross", "garchomp", "lucario", "zoroark", "volcarona", "greninja", "sylveon",
                "decidueye", "mimikyu", "dragapult", "ceruledge", "tinkaton"
        };
        for (String name : species) {
            crate.rewards.add(pokemon(name, "Shiny " + title(name), "SHINY", 1.0D, name + " level=50 shiny=true"));
        }
        crate.rewards.add(gems("shiny_gems", "500 Gems", "RARE", 3.0D, 500L));
        return crate.normalized(crate.id);
    }

    private static CrateDefinition voteCrate() {
        CrateDefinition crate = base("vote", "Vote Crate", "VOTE", "vote_key", "Vote Key", 0L);
        crate.rewards.add(item("rare_candy", "8 Rare Candy", "UNCOMMON", 30.0D, "cobblemon:rare_candy", 8));
        crate.rewards.add(item("ultra_balls", "16 Ultra Balls", "COMMON", 35.0D, "cobblemon:ultra_ball", 16));
        crate.rewards.add(item("exp_candy", "8 Exp. Candy L", "UNCOMMON", 20.0D, "cobblemon:exp_candy_l", 8));
        crate.rewards.add(gems("vote_gems", "100 Gems", "RARE", 10.0D, 100L));
        crate.rewards.add(pokemon("vote_shiny", "Random Shiny Pikachu", "EPIC", 2.0D, "pikachu level=25 shiny=true"));
        crate.rewards.add(key("legendary_key", "Legendary Key", "LEGENDARY", 1.0D, "legendary_key", 1));
        return crate.normalized(crate.id);
    }

    private static CrateDefinition base(String id, String name, String style, String keyId, String keyName, long gemCost) {
        CrateDefinition crate = new CrateDefinition();
        crate.id = id;
        crate.displayName = name;
        crate.style = style;
        crate.keyId = keyId;
        crate.keyName = keyName;
        crate.gemCost = gemCost;
        crate.rewards = new ArrayList<>();
        return crate;
    }

    private static RewardDefinition pokemon(String id, String name, String rarity, double weight, String properties) {
        RewardDefinition reward = reward(id, name, rarity, weight, RewardType.POKEMON);
        reward.pokemon = properties;
        reward.item = "cobblemon:poke_ball";
        return reward;
    }

    private static RewardDefinition item(String id, String name, String rarity, double weight, String item, int count) {
        RewardDefinition reward = reward(id, name, rarity, weight, RewardType.ITEM);
        reward.item = item;
        reward.count = count;
        return reward;
    }

    private static RewardDefinition gems(String id, String name, String rarity, double weight, long amount) {
        RewardDefinition reward = reward(id, name, rarity, weight, RewardType.GEMS);
        reward.gems = amount;
        reward.item = "minecraft:emerald";
        return reward;
    }

    private static RewardDefinition key(String id, String name, String rarity, double weight, String keyId, int amount) {
        RewardDefinition reward = reward(id, name, rarity, weight, RewardType.KEY);
        reward.keyId = keyId;
        reward.keyAmount = amount;
        reward.item = "minecraft:tripwire_hook";
        return reward;
    }

    private static RewardDefinition reward(String id, String name, String rarity, double weight, RewardType type) {
        RewardDefinition reward = new RewardDefinition();
        reward.id = id;
        reward.displayName = name;
        reward.rarity = rarity;
        reward.weight = weight;
        reward.type = type.name();
        return reward;
    }

    private static String title(String id) {
        String[] words = id.split("_");
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) result.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return String.join(" ", result)
                .replace("Hooh", "Ho-Oh")
                .replace("Typenull", "Type: Null")
                .replace("Tapukoko", "Tapu Koko")
                .replace("Tapulele", "Tapu Lele")
                .replace("Tapubulu", "Tapu Bulu")
                .replace("Tapufini", "Tapu Fini");
    }
}
