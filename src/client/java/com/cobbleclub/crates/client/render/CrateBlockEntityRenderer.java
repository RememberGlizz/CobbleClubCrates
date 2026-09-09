package com.cobbleclub.crates.client.render;

import com.cobbleclub.crates.block.entity.CrateBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

public final class CrateBlockEntityRenderer implements BlockEntityRenderer<CrateBlockEntity> {
    private final TextRenderer textRenderer;

    public CrateBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.textRenderer = context.getTextRenderer();
    }

    @Override
    public void render(CrateBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertices, int light, int overlay) {
        if (entity.getCrateId().isBlank()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        matrices.push();
        matrices.translate(0.5, 2.35, 0.5);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
        matrices.scale(-0.025F, -0.025F, 0.025F);
        Text title = Text.literal(entity.getDisplayName());
        float x = -textRenderer.getWidth(title) / 2.0F;
        textRenderer.draw(title, x, 0F, 0xFFFF762B, true,
                matrices.peek().getPositionMatrix(), vertices,
                TextRenderer.TextLayerType.SEE_THROUGH, 0x00000000,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);
        matrices.pop();
    }
}
