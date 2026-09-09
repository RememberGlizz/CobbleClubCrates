package com.cobbleclub.crates.network;

import com.cobbleclub.crates.CobbleClubCrates;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record CratePreviewPayload(
        String crateId,
        String displayName,
        String style,
        String keyName,
        int keys,
        long gemCost,
        long gems,
        List<RewardView> rewards
) implements CustomPayload {
    public static final Id<CratePreviewPayload> ID = new Id<>(CobbleClubCrates.id("crate_preview"));
    public static final PacketCodec<RegistryByteBuf, CratePreviewPayload> CODEC = PacketCodec.of(CratePreviewPayload::write, CratePreviewPayload::new);

    private CratePreviewPayload(RegistryByteBuf buffer) {
        this(
                buffer.readString(64), buffer.readString(256), buffer.readString(32),
                buffer.readString(128), buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong(),
                readRewards(buffer)
        );
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeString(crateId, 64);
        buffer.writeString(displayName, 256);
        buffer.writeString(style, 32);
        buffer.writeString(keyName, 128);
        buffer.writeVarInt(keys);
        buffer.writeVarLong(gemCost);
        buffer.writeVarLong(gems);
        buffer.writeVarInt(rewards.size());
        for (RewardView reward : rewards) reward.write(buffer);
    }

    static List<RewardView> readRewards(RegistryByteBuf buffer) {
        int size = Math.min(512, Math.max(0, buffer.readVarInt()));
        List<RewardView> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(RewardView.read(buffer));
        return List.copyOf(result);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
