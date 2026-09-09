package com.cobbleclub.crates.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WeightedSelectorTest {
    @Test
    void rejectsEmptyRewards() {
        assertThrows(IllegalArgumentException.class, () -> WeightedSelector.select(List.of(), new Random(1L)));
    }

    @Test
    void ignoresInvalidWeights() {
        RewardDefinition invalid = reward("invalid", Double.NaN);
        RewardDefinition winner = reward("winner", 1.0D);

        for (int i = 0; i < 100; i++) {
            assertEquals(winner, WeightedSelector.select(List.of(invalid, winner), new Random(i)));
        }
    }

    @Test
    void followsConfiguredWeightRatio() {
        RewardDefinition common = reward("common", 9.0D);
        RewardDefinition rare = reward("rare", 1.0D);
        Random random = new Random(42L);
        int rareWins = 0;

        for (int i = 0; i < 20_000; i++) {
            if (WeightedSelector.select(List.of(common, rare), random) == rare) rareWins++;
        }

        assertTrue(rareWins > 1_700 && rareWins < 2_300, "unexpected rare wins: " + rareWins);
    }

    private static RewardDefinition reward(String id, double weight) {
        RewardDefinition reward = new RewardDefinition();
        reward.id = id;
        reward.displayName = id;
        reward.weight = weight;
        return reward;
    }
}
