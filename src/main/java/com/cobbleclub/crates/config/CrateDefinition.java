package com.cobbleclub.crates.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CrateDefinition {
    public String id = "crate";
    public String displayName = "Crate";
    public String style = "BASIC";
    public String keyId = "crate_key";
    public String keyName = "Crate Key";
    public long gemCost = 500L;
    public boolean enabled = true;
    public int cooldownSeconds = 4;
    public List<RewardDefinition> rewards = new ArrayList<>();

    public CrateDefinition normalized(String fallbackId) {
        if (id == null || id.isBlank()) id = fallbackId;
        id = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (displayName == null || displayName.isBlank()) displayName = id;
        if (style == null || style.isBlank()) style = "BASIC";
        style = style.toUpperCase(Locale.ROOT);
        if (keyId == null || keyId.isBlank()) keyId = id + "_key";
        keyId = keyId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
        if (keyName == null || keyName.isBlank()) keyName = displayName + " Key";
        if (gemCost < 0L) gemCost = 0L;
        if (cooldownSeconds < 0) cooldownSeconds = 0;
        if (rewards == null) rewards = new ArrayList<>();
        for (int i = 0; i < rewards.size(); i++) rewards.get(i).normalized(i);
        return this;
    }
}
