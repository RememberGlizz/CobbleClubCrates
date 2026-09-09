package com.cobbleclub.crates.network;

import net.minecraft.network.RegistryByteBuf;

public record CrateSummary(
        String id,
        String displayName,
        String style,
        String keyName,
        int keys,
        long gemCost
) {
    public void write(RegistryByteBuf buffer) {
        buffer.writeString(id);
        buffer.writeString(displayName);
        buffer.writeString(style);
        buffer.writeString(keyName);
        buffer.writeInt(keys);
        buffer.writeVarLong(gemCost);
    }

    public static CrateSummary read(RegistryByteBuf buffer) {
        return new CrateSummary(
                buffer.readString(),
                buffer.readString(),
                buffer.readString(),
                buffer.readString(),
                buffer.readInt(),
                buffer.readVarLong()
        );
    }
}