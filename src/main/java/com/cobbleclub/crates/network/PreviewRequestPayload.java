package com.cobbleclub.crates.network;

import com.cobbleclub.crates.CobbleClubCrates;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record PreviewRequestPayload(String crateId) implements CustomPayload {
    public static final Id<PreviewRequestPayload> ID = new Id<>(CobbleClubCrates.id("preview_request"));
    public static final PacketCodec<RegistryByteBuf, PreviewRequestPayload> CODEC = PacketCodec.of(PreviewRequestPayload::write, PreviewRequestPayload::new);

    private PreviewRequestPayload(RegistryByteBuf buffer) {
        this(buffer.readString(64));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeString(crateId, 64);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
