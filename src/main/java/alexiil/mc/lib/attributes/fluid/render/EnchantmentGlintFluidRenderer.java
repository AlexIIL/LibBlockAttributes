package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class EnchantmentGlintFluidRenderer extends FluidVolumeRenderer {

    public static final EnchantmentGlintFluidRenderer INSTANCE = new EnchantmentGlintFluidRenderer();

    protected EnchantmentGlintFluidRenderer() {}

    @Override
    public void render(FluidVolume fluid, List<FluidRenderFace> faces, double x, double y, double z) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBufferBuilder();

        int colour = fluid.getRenderColor();
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = (colour >> 0) & 0xFF;
        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas().getSprite(fluid.getSprite());
        ComponentRenderFaces fc = splitFacesComponent(faces);

        bb.begin(GL11.GL_QUADS, VertexFormats.POSITION_UV_COLOR);
        bb.setOffset(x, y, z);

        for (FluidRenderFace f : fc.split) {
            vertex(bb, f.x0, f.y0, f.z0, sprite.getU(f.u0), sprite.getV(f.v0), r, g, b);
            vertex(bb, f.x1, f.y1, f.z1, sprite.getU(f.u1), sprite.getV(f.v1), r, g, b);
            vertex(bb, f.x2, f.y2, f.z2, sprite.getU(f.u2), sprite.getV(f.v2), r, g, b);
            vertex(bb, f.x3, f.y3, f.z3, sprite.getU(f.u3), sprite.getV(f.v3), r, g, b);
        }
        bb.setOffset(0, 0, 0);
        tess.draw();

        ItemRenderer.renderGlint(MinecraftClient.getInstance().getTextureManager(), () -> {
            bb.begin(GL11.GL_QUADS, VertexFormats.POSITION_UV_COLOR);
            bb.setOffset(x, y, z);

            for (FluidRenderFace f : fc.splitExceptTextures) {
                glintVertex(bb, f.x0, f.y0, f.z0, sprite.getU(f.u0), sprite.getV(f.v0));
                glintVertex(bb, f.x1, f.y1, f.z1, sprite.getU(f.u1), sprite.getV(f.v1));
                glintVertex(bb, f.x2, f.y2, f.z2, sprite.getU(f.u2), sprite.getV(f.v2));
                glintVertex(bb, f.x3, f.y3, f.z3, sprite.getU(f.u3), sprite.getV(f.v3));
            }
            bb.setOffset(0, 0, 0);
            tess.draw();
        }, 8);

        // opengl cleanup
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
    }

    private static void glintVertex(BufferBuilder bb, double x, double y, double z, float u, float v) {
        bb.vertex(x, y, z);
        bb.texture(u, v);
        bb.color(0x80, 0x40, 0xCC, 0xFF);
        bb.next();
    }
}
