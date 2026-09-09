package com.cobbleclub.crates.network;

import com.cobbleclub.crates.CobbleClubCrates;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record CrateResultPayload(
        String crateId,
        String displayName,
        RewardView winner,
        List<RewardView> sequence,
        long gems,
        int keys
) implements CustomPayload {
    public static final Id<CrateResultPayload> ID = new Id<>(CobbleClubCrates.id("crate_result"));
    public static final PacketCodec<RegistryByteBuf, CrateResultPayload> CODEC = PacketCodec.of(CrateResultPayload::write, CrateResultPayload::new);

    private CrateResultPayload(RegistryByteBuf buffer) {
        this(
                buffer.readString(64), buffer.readString(256), RewardView.read(buffer),
                CratePreviewPayload.readRewards(buffer), buffer.readVarLong(), buffer.readVarInt()
        );
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeString(crateId, 64);
        buffer.writeString(displayName, 256);
        winner.write(buffer);
        buffer.writeVarInt(sequence.size());
        for (RewardView reward : sequence) reward.write(buffer);
        buffer.writeVarLong(gems);
        buffer.writeVarInt(keys);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
