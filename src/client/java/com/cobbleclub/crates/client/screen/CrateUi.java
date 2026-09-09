package com.cobbleclub.crates.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class CrateUi {
    static final int PANEL = 0xF22E3035;
    static final int PANEL_LIGHT = 0xFF41444A;
    static final int SLOT = 0xFF383A3F;
    static final int SLOT_HOVER = 0xFF50535A;
    static final int BORDER = 0xFF1D1E22;
    static final int ORANGE = 0xFFFF762B;
    static final int PURPLE = 0xFF9D79E8;
    static final int GREEN = 0xFF6BE26B;
    static final int RED = 0xFFFF6868;

    private CrateUi() {}

    static void panel(DrawContext context, int left, int top, int right, int bottom) {
        context.fill(left - 4, top - 4, right + 4, bottom + 4, BORDER);
        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, right, top + 4, 0xFF666970);
        context.fill(left, bottom - 4, right, bottom, 0xFF25272B);
    }

    static void slot(DrawContext context, int x, int y, int size, boolean hovered) {
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, BORDER);
        context.fill(x, y, x + size, y + size, hovered ? SLOT_HOVER : SLOT);
    }

    static void centered(DrawContext context, Screen screen, String value, int centerX, int y, int color) {
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(value), centerX, y, color);
    }

    static int rarityColor(String rarity) {
        return switch (rarity == null ? "" : rarity.toUpperCase()) {
            case "MYTHIC", "LEGENDARY" -> ORANGE;
            case "EPIC" -> 0xFFC477FF;
            case "RARE" -> 0xFF61B8FF;
            case "UNCOMMON" -> 0xFF6BE26B;
            default -> 0xFFFFFFFF;
        };
    }

    static Text styledTitle(String title) {
        return Text.literal(title).formatted(Formatting.GOLD, Formatting.BOLD);
    }
}
