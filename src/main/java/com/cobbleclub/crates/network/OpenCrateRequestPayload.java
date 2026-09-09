package com.cobbleclub.crates.network;

import com.cobbleclub.crates.CobbleClubCrates;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record OpenCrateRequestPayload(String crateId, String payment) implements CustomPayload {
    public static final Id<OpenCrateRequestPayload> ID = new Id<>(CobbleClubCrates.id("open_request"));
    public static final PacketCodec<RegistryByteBuf, OpenCrateRequestPayload> CODEC = PacketCodec.of(OpenCrateRequestPayload::write, OpenCrateRequestPayload::new);

    private OpenCrateRequestPayload(RegistryByteBuf buffer) {
        this(buffer.readString(64), buffer.readString(16));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeString(crateId, 64);
        buffer.writeString(payment, 16);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
