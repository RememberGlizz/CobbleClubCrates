package com.cobbleclub.crates.network;

import com.cobbleclub.crates.config.RewardDefinition;
import net.minecraft.network.RegistryByteBuf;

public record RewardView(
        String type,
        String displayName,
        String rarity,
        String pokemon,
        String item,
        int count,
        double weight
) {
    public static RewardView from(RewardDefinition reward) {
        return new RewardView(
                reward.rewardType().name(), reward.displayName, reward.rarity,
                reward.pokemon, reward.item, reward.count, reward.weight
        );
    }

    public void write(RegistryByteBuf buffer) {
        buffer.writeString(type, 16);
        buffer.writeString(displayName, 256);
        buffer.writeString(rarity, 32);
        buffer.writeString(pokemon, 512);
        buffer.writeString(item, 256);
        buffer.writeVarInt(Math.max(0, count));
        buffer.writeDouble(weight);
    }

    public static RewardView read(RegistryByteBuf buffer) {
        return new RewardView(
                buffer.readString(16), buffer.readString(256), buffer.readString(32),
                buffer.readString(512), buffer.readString(256),
                buffer.readVarInt(), buffer.readDouble()
        );
    }
}
