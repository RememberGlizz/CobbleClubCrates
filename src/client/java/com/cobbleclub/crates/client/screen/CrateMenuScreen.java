package com.cobbleclub.crates.client.screen;

import com.cobbleclub.crates.block.ModBlocks;
import com.cobbleclub.crates.network.CrateMenuPayload;
import com.cobbleclub.crates.network.CrateSummary;
import com.cobbleclub.crates.network.PreviewRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class CrateMenuScreen extends Screen {
    private final CrateMenuPayload payload;
    private int left;
    private int top;

    public CrateMenuScreen(CrateMenuPayload payload) {
        super(Text.literal("CobbleClub Crates"));
        this.payload = payload;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        int columns = Math.min(3, Math.max(1, payload.crates().size()));
        int rows = Math.max(1, (payload.crates().size() + columns - 1) / columns);
        int panelWidth = Math.min(width - 24, columns * 126 + 28);
        int panelHeight = Math.min(height - 24, rows * 94 + 70);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        CrateUi.panel(context, left, top, left + panelWidth, top + panelHeight);
        CrateUi.centered(context, this, "COBBLECLUB CRATES", width / 2, top + 14, CrateUi.ORANGE);
        context.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(payload.gems())), left + panelWidth - 42, top + 14, CrateUi.PURPLE);

        for (int i = 0; i < payload.crates().size(); i++) {
            int col = i % columns;
            int row = i / columns;
            int x = left + 14 + col * 126;
            int y = top + 42 + row * 94;
            boolean hovered = mouseX >= x && mouseX < x + 112 && mouseY >= y && mouseY < y + 80;
            CrateUi.slot(context, x, y, 80, hovered);
            context.fill(x + 81, y, x + 112, y + 80, hovered ? CrateUi.SLOT_HOVER : CrateUi.SLOT);
            CrateSummary crate = payload.crates().get(i);
            ItemStack icon = new ItemStack(ModBlocks.byStyle(crate.style()));
            context.getMatrices().push();
            context.getMatrices().translate(x + 16, y + 11, 50);
            context.getMatrices().scale(3F, 3F, 1F);
            context.drawItem(icon, 0, 0);
            context.getMatrices().pop();
            String name = crate.displayName().replace(" Crate", "");
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(name), x + 56, y + 58, CrateUi.ORANGE);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(crate.keys() + " key" + (crate.keys() == 1 ? "" : "s")), x + 56, y + 68, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int columns = Math.min(3, Math.max(1, payload.crates().size()));
        for (int i = 0; i < payload.crates().size(); i++) {
            int x = left + 14 + (i % columns) * 126;
            int y = top + 42 + (i / columns) * 94;
            if (mouseX >= x && mouseX < x + 112 && mouseY >= y && mouseY < y + 80) {
                ClientPlayNetworking.send(new PreviewRequestPayload(payload.crates().get(i).id()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
