package com.cobbleclub.crates.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RewardDefinitionTest {
    @Test
    void normalizesUnsafeValues() {
        RewardDefinition reward = new RewardDefinition();
        reward.id = "";
        reward.displayName = "";
        reward.weight = Double.POSITIVE_INFINITY;
        reward.count = -10;
        reward.gems = -4L;
        reward.keyAmount = -2;

        reward.normalized(7);

        assertEquals("reward_7", reward.id);
        assertEquals("reward_7", reward.displayName);
        assertEquals(1.0D, reward.weight);
        assertEquals(1, reward.count);
        assertEquals(0L, reward.gems);
        assertEquals(0, reward.keyAmount);
    }
}
