package com.cobbleclub.crates.config;

import java.util.Locale;

public final class RewardDefinition {
    public String id = "reward";
    public String type = "ITEM";
    public String displayName = "Reward";
    public String rarity = "COMMON";
    public double weight = 1.0D;
    public String pokemon = "";
    public String item = "minecraft:diamond";
    public int count = 1;
    public String command = "";
    public long gems = 0L;
    public String keyId = "";
    public int keyAmount = 0;
    public boolean broadcast = false;

    public RewardType rewardType() {
        try {
            return RewardType.valueOf(type == null ? "ITEM" : type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return RewardType.ITEM;
        }
    }

    public RewardDefinition normalized(int index) {
        if (id == null || id.isBlank()) id = "reward_" + index;
        if (displayName == null || displayName.isBlank()) displayName = id;
        if (rarity == null || rarity.isBlank()) rarity = "COMMON";
        if (!Double.isFinite(weight) || weight <= 0.0D) weight = 1.0D;
        if (pokemon == null) pokemon = "";
        if (item == null || item.isBlank()) item = "minecraft:diamond";
        if (count < 1) count = 1;
        if (command == null) command = "";
        if (gems < 0L) gems = 0L;
        if (keyId == null) keyId = "";
        if (keyAmount < 0) keyAmount = 0;
        return this;
    }
}
