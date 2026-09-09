package com.cobbleclub.crates.network;

import com.cobbleclub.crates.CobbleClubCrates;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record CrateMenuPayload(long gems, List<CrateSummary> crates) implements CustomPayload {
    public static final Id<CrateMenuPayload> ID = new Id<>(CobbleClubCrates.id("crate_menu"));
    public static final PacketCodec<RegistryByteBuf, CrateMenuPayload> CODEC = PacketCodec.of(CrateMenuPayload::write, CrateMenuPayload::new);

    private CrateMenuPayload(RegistryByteBuf buffer) {
        this(buffer.readVarLong(), readCrates(buffer));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarLong(gems);
        buffer.writeVarInt(crates.size());
        for (CrateSummary crate : crates) crate.write(buffer);
    }

    private static List<CrateSummary> readCrates(RegistryByteBuf buffer) {
        int size = Math.min(128, Math.max(0, buffer.readVarInt()));
        List<CrateSummary> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(CrateSummary.read(buffer));
        return List.copyOf(result);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
