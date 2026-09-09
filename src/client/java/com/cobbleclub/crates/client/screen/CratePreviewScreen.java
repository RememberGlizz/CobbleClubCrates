package com.cobbleclub.crates.client.screen;

import com.cobbleclub.crates.network.CratePreviewPayload;
import com.cobbleclub.crates.network.OpenCrateRequestPayload;
import com.cobbleclub.crates.network.RewardView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

public final class CratePreviewScreen extends Screen {
    private static final int MAX_COLUMNS = 9;
    private static final int MAX_ROWS = 5;
    private static final int MIN_SLOT_SIZE = 24;
    private static final int MAX_SLOT_SIZE = 42;
    private final CratePreviewPayload payload;
    private final PokemonRewardRenderer rewardRenderer = new PokemonRewardRenderer();
    private int page;
    private int left;
    private int top;
    private int slotSize;
    private int columns;
    private int rows;
    private int pageSize;

    public CratePreviewScreen(CratePreviewPayload payload) {
        super(Text.literal(payload.displayName() + " Preview"));
        this.payload = payload;
    }

    /** Sizes the grid to the actual reward count (capped at MAX_COLUMNS x MAX_ROWS per page)
     *  and shrinks slots as needed so the panel never exceeds the available window space. */
    private void updateLayout() {
        int rewardCount = Math.max(1, payload.rewards().size());
        columns = Math.max(1, Math.min(MAX_COLUMNS, rewardCount));
        rows = Math.max(1, Math.min(MAX_ROWS, (rewardCount + columns - 1) / columns));
        pageSize = columns * rows;

        int availableWidth = Math.max(columns * MIN_SLOT_SIZE + 28, width - 24);
        int availableHeight = Math.max(rows * MIN_SLOT_SIZE + 84, height - 24);
        int fitWidth = (availableWidth - 28) / columns;
        int fitHeight = (availableHeight - 84) / rows;

        slotSize = Math.max(MIN_SLOT_SIZE, Math.min(MAX_SLOT_SIZE, Math.min(fitWidth, fitHeight)));
        // Hard cap: never let the panel exceed the window, even on very small screens/GUI scales.
        int hardCapWidth = Math.max(1, (width - 24 - 28) / columns);
        int hardCapHeight = Math.max(1, (height - 24 - 84) / rows);
        slotSize = Math.min(slotSize, Math.min(hardCapWidth, hardCapHeight));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        updateLayout();
        int panelWidth = columns * slotSize + 28;
        int panelHeight = rows * slotSize + 84;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        CrateUi.panel(context, left, top, left + panelWidth, top + panelHeight);
        CrateUi.centered(context, this, payload.displayName() + " Preview", width / 2, top + 13, CrateUi.ORANGE);

        int start = page * pageSize;
        int gridX = left + 14;
        int gridY = top + 34;
        RewardView hoveredReward = null;
        for (int index = 0; index < pageSize; index++) {
            int rewardIndex = start + index;
            int x = gridX + (index % columns) * slotSize;
            int y = gridY + (index / columns) * slotSize;
            boolean hovered = mouseX >= x && mouseX < x + slotSize - 2 && mouseY >= y && mouseY < y + slotSize - 2;
            CrateUi.slot(context, x, y, slotSize - 2, hovered);
            if (rewardIndex >= payload.rewards().size()) continue;
            RewardView reward = payload.rewards().get(rewardIndex);
            rewardRenderer.draw(context, reward, x + 1, y + 1, slotSize - 4, delta);
            context.fill(x, y + slotSize - 5, x + slotSize - 2, y + slotSize - 2, CrateUi.rarityColor(reward.rarity()));
            if (hovered) hoveredReward = reward;
        }

        int buttonY = top + panelHeight - 38;
        int centerX = width / 2;
        if (payload.gemCost() > 0) {
            drawAction(context, centerX - 112, buttonY, 70, "Key (" + payload.keys() + ")", new ItemStack(Items.TRIPWIRE_HOOK), payload.keys() > 0, mouseX, mouseY);
            drawAction(context, centerX - 35, buttonY, 70, String.valueOf(payload.gemCost()), new ItemStack(Items.EMERALD), payload.gems() >= payload.gemCost(), mouseX, mouseY);
            drawAction(context, centerX + 42, buttonY, 70, "Close", new ItemStack(Items.BARRIER), true, mouseX, mouseY);
        } else {
            drawAction(context, centerX - 74, buttonY, 70, "Key (" + payload.keys() + ")", new ItemStack(Items.TRIPWIRE_HOOK), payload.keys() > 0, mouseX, mouseY);
            drawAction(context, centerX + 4, buttonY, 70, "Close", new ItemStack(Items.BARRIER), true, mouseX, mouseY);
        }

        int pages = Math.max(1, (payload.rewards().size() + pageSize - 1) / pageSize);
        if (pages > 1) {
            context.drawTextWithShadow(textRenderer, Text.literal("<"), left + 7, buttonY + 10, page > 0 ? 0xFFFFFFFF : 0xFF777777);
            context.drawTextWithShadow(textRenderer, Text.literal(">"), left + panelWidth - 13, buttonY + 10, page + 1 < pages ? 0xFFFFFFFF : 0xFF777777);
            CrateUi.centered(context, this, (page + 1) + "/" + pages, width / 2, buttonY - 10, 0xFFCCCCCC);
        }
        if (hoveredReward != null) {
            double chance = totalWeight() <= 0D ? 0D : (hoveredReward.weight() / totalWeight()) * 100D;
            int hoveredColor = CrateUi.rarityColor(hoveredReward.rarity());
            List<Text> tooltip = List.of(
                    Text.literal(hoveredReward.displayName()).styled(style -> style.withColor(hoveredColor)),
                    Text.literal(hoveredReward.rarity() + " • " + String.format("%.2f%% chance", chance))
            );
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void drawAction(DrawContext context, int x, int y, int width, String label, ItemStack icon,
                            boolean enabled, int mouseX, int mouseY) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 28;
        context.fill(x, y, x + width, y + 28, enabled ? (hovered ? 0xFF62558A : 0xFF4A4263) : 0xFF333439);
        context.fill(x, y, x + width, y + 2, enabled ? CrateUi.PURPLE : 0xFF555555);
        context.drawItem(icon, x + 4, y + 6);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 23, y + 10, enabled ? 0xFFFFFFFF : 0xFF888888);
    }

    private double totalWeight() {
        return payload.rewards().stream().mapToDouble(RewardView::weight).sum();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelWidth = columns * slotSize + 28;
        int panelHeight = rows * slotSize + 84;
        int buttonY = top + panelHeight - 38;
        int centerX = width / 2;
        int keyX = payload.gemCost() > 0 ? centerX - 112 : centerX - 74;
        int closeX = payload.gemCost() > 0 ? centerX + 42 : centerX + 4;
        if (inside(mouseX, mouseY, keyX, buttonY, 70, 28) && payload.keys() > 0) {
            ClientPlayNetworking.send(new OpenCrateRequestPayload(payload.crateId(), "KEY"));
            return true;
        }
        if (inside(mouseX, mouseY, centerX - 35, buttonY, 70, 28) && payload.gemCost() > 0 && payload.gems() >= payload.gemCost()) {
            ClientPlayNetworking.send(new OpenCrateRequestPayload(payload.crateId(), "GEMS"));
            return true;
        }
        if (inside(mouseX, mouseY, closeX, buttonY, 70, 28)) {
            close();
            return true;
        }
        int pages = Math.max(1, (payload.rewards().size() + pageSize - 1) / pageSize);
        if (mouseY >= buttonY && mouseY < buttonY + 28 && mouseX >= left && mouseX < left + 22 && page > 0) {
            page--;
            return true;
        }
        if (mouseY >= buttonY && mouseY < buttonY + 28 && mouseX >= left + panelWidth - 22 && mouseX < left + panelWidth && page + 1 < pages) {
            page++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}