package com.cobbleclub.crates.economy;

import com.cobbleclub.server.data.PlayerDataStore;
import com.cobbleclub.server.service.EconomyService;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for gems and crate keys: CobbleClub-Server's economy.
 * The crates mod no longer keeps its own copy of this data - it reads and writes
 * the main mod's {@link PlayerDataStore.PlayerData} directly so balances always match
 * what the main mod (daily rewards, playtime, votes, /pay, etc.) has actually granted.
 */
public final class ServerEconomy {
    private ServerEconomy() {}

    public static long gems(ServerPlayerEntity player) {
        return EconomyService.gems(player);
    }

    public static long addGems(ServerPlayerEntity player, long delta) {
        if (delta < 0) {
            EconomyService.withdrawGems(player, -delta);
        } else {
            EconomyService.depositGems(player, delta);
        }
        return EconomyService.gems(player);
    }

    public static long setGems(ServerPlayerEntity player, long amount) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        data.gems = Math.max(0L, amount);
        data.revision++;
        PlayerDataStore.save();
        return data.gems;
    }

    public static boolean spendGems(ServerPlayerEntity player, long amount) {
        if (amount < 0L) return false;
        return EconomyService.withdrawGems(player, amount);
    }

    public static int keys(ServerPlayerEntity player, String keyId) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        return Math.max(0, data.crateKeys.getOrDefault(normalize(keyId), 0));
    }

    public static int addKeys(ServerPlayerEntity player, String keyId, int delta) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        String normalized = normalize(keyId);
        long result = (long) data.crateKeys.getOrDefault(normalized, 0) + delta;
        int value = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, result));
        if (value == 0) data.crateKeys.remove(normalized); else data.crateKeys.put(normalized, value);
        data.revision++;
        PlayerDataStore.save();
        return value;
    }

    public static int setKeys(ServerPlayerEntity player, String keyId, int amount) {
        PlayerDataStore.PlayerData data = EconomyService.data(player);
        String normalized = normalize(keyId);
        int value = Math.max(0, amount);
        if (value == 0) data.crateKeys.remove(normalized); else data.crateKeys.put(normalized, value);
        data.revision++;
        PlayerDataStore.save();
        return value;
    }

    public static boolean spendKey(ServerPlayerEntity player, String keyId) {
        if (keys(player, keyId) <= 0) return false;
        addKeys(player, keyId, -1);
        return true;
    }

    public static Map<String, Integer> keySnapshot(ServerPlayerEntity player) {
        return Map.copyOf(EconomyService.data(player).crateKeys);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }
}
