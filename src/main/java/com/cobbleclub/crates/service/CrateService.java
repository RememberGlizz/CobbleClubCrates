package com.cobbleclub.crates.service;

import com.cobbleclub.crates.CobbleClubCrates;
import com.cobbleclub.crates.config.CrateConfigManager;
import com.cobbleclub.crates.config.CrateDefinition;
import com.cobbleclub.crates.config.RewardDefinition;
import com.cobbleclub.crates.config.WeightedSelector;
import com.cobbleclub.crates.economy.ServerEconomy;
import com.cobbleclub.crates.network.CrateMenuPayload;
import com.cobbleclub.crates.network.CratePreviewPayload;
import com.cobbleclub.crates.network.CrateResultPayload;
import com.cobbleclub.crates.network.CrateSummary;
import com.cobbleclub.crates.network.RewardView;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CrateService {
    private static final Map<UUID, Long> NEXT_OPEN = new HashMap<>();

    private CrateService() {}

    public static void sendMenu(ServerPlayerEntity player) {
        if (!canUseClient(player, CrateMenuPayload.ID)) return;
        List<CrateSummary> summaries = CrateConfigManager.all().stream()
                .map(crate -> new CrateSummary(
                        crate.id, crate.displayName, crate.style, crate.keyName,
                        ServerEconomy.keys(player, crate.keyId), crate.gemCost
                )).toList();
        ServerPlayNetworking.send(player, new CrateMenuPayload(ServerEconomy.gems(player), summaries));
    }

    public static void sendPreview(ServerPlayerEntity player, String crateId) {
        CrateDefinition crate = CrateConfigManager.get(crateId).orElse(null);
        if (crate == null || !crate.enabled) {
            player.sendMessage(Text.literal("Unknown or disabled crate: " + crateId).formatted(Formatting.RED));
            return;
        }
        if (!canUseClient(player, CratePreviewPayload.ID)) return;
        List<RewardView> rewards = crate.rewards.stream().map(RewardView::from).toList();
        ServerPlayNetworking.send(player, new CratePreviewPayload(
                crate.id, crate.displayName, crate.style, crate.keyName,
                ServerEconomy.keys(player, crate.keyId), crate.gemCost,
                ServerEconomy.gems(player), rewards
        ));
    }

    public static void open(ServerPlayerEntity player, String crateId, String requestedPayment) {
        CrateDefinition crate = CrateConfigManager.get(crateId).orElse(null);
        if (crate == null || !crate.enabled || crate.rewards.isEmpty()) {
            player.sendMessage(Text.literal("That crate is unavailable.").formatted(Formatting.RED));
            return;
        }

        long now = System.currentTimeMillis();
        long next = NEXT_OPEN.getOrDefault(player.getUuid(), 0L);
        if (next > now) {
            long seconds = Math.max(1L, (next - now + 999L) / 1000L);
            player.sendMessage(Text.literal("Please wait " + seconds + " second(s) before opening another crate.")
                    .formatted(Formatting.RED));
            return;
        }

        Payment payment = Payment.parse(requestedPayment);
        if (!takePayment(player, crate, payment)) return;

        RewardDefinition winner;
        try {
            winner = WeightedSelector.select(crate.rewards, ThreadLocalRandom.current());
            if (!grant(player, winner)) {
                refund(player, crate, payment);
                player.sendMessage(Text.literal("The selected reward is invalid. Your payment was refunded.")
                        .formatted(Formatting.RED));
                return;
            }
        } catch (Exception exception) {
            refund(player, crate, payment);
            CobbleClubCrates.LOGGER.error("Unable to open crate '{}' for {}", crate.id, player.getName().getString(), exception);
            player.sendMessage(Text.literal("The crate could not open. Your payment was refunded.")
                    .formatted(Formatting.RED));
            return;
        }

        NEXT_OPEN.put(player.getUuid(), now + crate.cooldownSeconds * 1000L);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.15F);
        player.sendMessage(Text.literal("You won " + winner.displayName + "!")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));

        if (winner.broadcast) {
            Text announcement = Text.literal(player.getName().getString() + " won " + winner.displayName
                    + " from " + crate.displayName + "!").formatted(Formatting.GOLD, Formatting.BOLD);
            player.getServer().getPlayerManager().broadcast(announcement, false);
        }

        if (ServerPlayNetworking.canSend(player, CrateResultPayload.ID)) {
            List<RewardView> sequence = buildSequence(crate, winner);
            ServerPlayNetworking.send(player, new CrateResultPayload(
                    crate.id, crate.displayName, RewardView.from(winner), sequence,
                    ServerEconomy.gems(player), ServerEconomy.keys(player, crate.keyId)
            ));
        }
    }

    private static boolean takePayment(ServerPlayerEntity player, CrateDefinition crate, Payment payment) {
        if (payment == Payment.KEY) {
            if (!ServerEconomy.spendKey(player, crate.keyId)) {
                player.sendMessage(Text.literal("You need a " + crate.keyName + " to open this crate.")
                        .formatted(Formatting.RED));
                return false;
            }
            return true;
        }

        if (crate.gemCost <= 0L) {
            player.sendMessage(Text.literal("This crate can only be opened with a key.").formatted(Formatting.RED));
            return false;
        }
        if (!ServerEconomy.spendGems(player, crate.gemCost)) {
            player.sendMessage(Text.literal("You need " + crate.gemCost + " Gems to open this crate.")
                    .formatted(Formatting.RED));
            return false;
        }
        return true;
    }

    private static void refund(ServerPlayerEntity player, CrateDefinition crate, Payment payment) {
        if (payment == Payment.KEY) ServerEconomy.addKeys(player, crate.keyId, 1);
        else ServerEconomy.addGems(player, crate.gemCost);
    }

    private static boolean grant(ServerPlayerEntity player, RewardDefinition reward) {
        return switch (reward.rewardType()) {
            case POKEMON -> givePokemon(player, reward.pokemon);
            case ITEM -> giveItem(player, reward.item, reward.count);
            case COMMAND -> runCommand(player, reward.command);
            case GEMS -> {
                ServerEconomy.addGems(player, reward.gems);
                yield true;
            }
            case KEY -> {
                if (reward.keyId == null || reward.keyId.isBlank() || reward.keyAmount <= 0) yield false;
                ServerEconomy.addKeys(player, reward.keyId, reward.keyAmount);
                yield true;
            }
        };
    }

    private static boolean givePokemon(ServerPlayerEntity player, String propertiesText) {
        if (propertiesText == null || propertiesText.isBlank()) return false;
        PokemonProperties properties = PokemonProperties.Companion.parse(propertiesText);
        if (!properties.hasSpecies()) return false;
        Pokemon pokemon = properties.create(player);
        boolean added = Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
        if (!added) added = Cobblemon.INSTANCE.getStorage().getPC(player).add(pokemon);
        return added;
    }

    private static boolean giveItem(ServerPlayerEntity player, String itemId, int count) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null || !Registries.ITEM.containsId(identifier)) return false;
        Item item = Registries.ITEM.get(identifier);
        int remaining = Math.max(1, count);
        while (remaining > 0) {
            int amount = Math.min(remaining, item.getMaxCount());
            ItemStack stack = new ItemStack(item, amount);
            if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
            remaining -= amount;
        }
        return true;
    }

    private static boolean runCommand(ServerPlayerEntity player, String command) {
        if (command == null || command.isBlank()) return false;
        String expanded = command.replace("{player}", player.getGameProfile().getName());
        if (expanded.startsWith("/")) expanded = expanded.substring(1);
        player.getServer().getCommandManager().executeWithPrefix(player.getServer().getCommandSource(), expanded);
        return true;
    }

    private static List<RewardView> buildSequence(CrateDefinition crate, RewardDefinition winner) {
        List<RewardView> sequence = new ArrayList<>(24);
        for (int i = 0; i < 23; i++) {
            sequence.add(RewardView.from(WeightedSelector.select(crate.rewards, ThreadLocalRandom.current())));
        }
        sequence.add(RewardView.from(winner));
        return List.copyOf(sequence);
    }

    private static boolean canUseClient(
            ServerPlayerEntity player, net.minecraft.network.packet.CustomPayload.Id<?> id) {
        if (ServerPlayNetworking.canSend(player, id)) return true;
        player.sendMessage(Text.literal("CobbleClub Crates must be installed in your client modpack.")
                .formatted(Formatting.RED));
        return false;
    }

    private enum Payment {
        KEY, GEMS;

        static Payment parse(String value) {
            return "GEMS".equalsIgnoreCase(value) ? GEMS : KEY;
        }
    }

}