package com.cobbleclub.crates.client.screen;

import com.cobbleclub.crates.network.CrateResultPayload;
import com.cobbleclub.crates.network.RewardView;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CrateOpeningScreen extends Screen {
    private final CrateResultPayload payload;
    private final PokemonRewardRenderer rewardRenderer = new PokemonRewardRenderer();
    private int ticks;

    public CrateOpeningScreen(CrateResultPayload payload) {
        super(Text.literal(payload.displayName()));
        this.payload = payload;
    }

    @Override
    public void tick() {
        if (!complete()) ticks++;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(360, width - 24);
        int panelHeight = Math.min(238, height - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        CrateUi.panel(context, left, top, left + panelWidth, top + panelHeight);
        CrateUi.centered(context, this, complete() ? "YOU WON!" : "OPENING " + payload.displayName().toUpperCase(), width / 2, top + 16, complete() ? CrateUi.GREEN : CrateUi.ORANGE);

        int rewardSize = 124;
        int rewardX = width / 2 - rewardSize / 2;
        int rewardY = top + 45;
        CrateUi.slot(context, rewardX, rewardY, rewardSize, false);
        RewardView shown = shownReward();
        rewardRenderer.draw(context, shown, rewardX + 5, rewardY + 5, rewardSize - 10, delta);
        context.fill(rewardX, rewardY + rewardSize - 6, rewardX + rewardSize, rewardY + rewardSize, CrateUi.rarityColor(shown.rarity()));
        CrateUi.centered(context, this, shown.displayName(), width / 2, rewardY + rewardSize + 10, CrateUi.rarityColor(shown.rarity()));

        if (complete()) {
            CrateUi.centered(context, this, "Click anywhere to continue", width / 2, top + panelHeight - 20, 0xFFDDDDDD);
        } else {
            int progressWidth = panelWidth - 48;
            int progress = (int) ((ticks / (double) finishTick()) * progressWidth);
            context.fill(left + 24, top + panelHeight - 20, left + 24 + progressWidth, top + panelHeight - 14, 0xFF27282D);
            context.fill(left + 24, top + panelHeight - 20, left + 24 + progress, top + panelHeight - 14, CrateUi.PURPLE);
        }
    }

    private RewardView shownReward() {
        if (complete() || payload.sequence().isEmpty()) return payload.winner();
        int index = Math.min(payload.sequence().size() - 1, ticks / 3);
        return payload.sequence().get(index);
    }

    private int finishTick() {
        return Math.max(1, payload.sequence().size() * 3);
    }

    private boolean complete() {
        return ticks >= finishTick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (complete()) {
            close();
            return true;
        }
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
