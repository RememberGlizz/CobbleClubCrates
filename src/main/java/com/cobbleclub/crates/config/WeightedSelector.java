package com.cobbleclub.crates.config;

import java.util.List;
import java.util.random.RandomGenerator;

public final class WeightedSelector {
    private WeightedSelector() {}

    public static RewardDefinition select(List<RewardDefinition> rewards, RandomGenerator random) {
        if (rewards == null || rewards.isEmpty()) {
            throw new IllegalArgumentException("A crate must contain at least one reward");
        }
        double total = 0.0D;
        for (RewardDefinition reward : rewards) {
            if (reward != null && Double.isFinite(reward.weight) && reward.weight > 0.0D) {
                total += reward.weight;
            }
        }
        if (!(total > 0.0D) || !Double.isFinite(total)) {
            throw new IllegalArgumentException("A crate must contain a positive finite reward weight");
        }

        double roll = random.nextDouble(total);
        RewardDefinition last = rewards.getLast();
        for (RewardDefinition reward : rewards) {
            if (reward == null || !Double.isFinite(reward.weight) || reward.weight <= 0.0D) continue;
            last = reward;
            roll -= reward.weight;
            if (roll < 0.0D) return reward;
        }
        return last;
    }
}
