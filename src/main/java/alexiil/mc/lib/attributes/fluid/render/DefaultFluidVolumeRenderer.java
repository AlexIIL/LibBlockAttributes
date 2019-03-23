package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class DefaultFluidVolumeRenderer extends FluidVolumeRenderer {

    public static final DefaultFluidVolumeRenderer INSTANCE = new DefaultFluidVolumeRenderer();

    protected DefaultFluidVolumeRenderer() {}

    @Override
    public void render(FluidVolume fluid, List<FluidRenderFace> faces, double x, double y, double z) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBufferBuilder();
        bb.begin(GL11.GL_QUADS, VertexFormats.POSITION_UV_COLOR);
        bb.setOffset(x, y, z);

        int colour = fluid.getRenderColor();
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = (colour >> 0) & 0xFF;
        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas().getSprite(fluid.getSprite());
        for (FluidRenderFace f : splitFaces(faces)) {
            vertex(bb, f.x0, f.y0, f.z0, sprite.getU(f.u0), sprite.getV(f.v0), r, g, b);
            vertex(bb, f.x1, f.y1, f.z1, sprite.getU(f.u1), sprite.getV(f.v1), r, g, b);
            vertex(bb, f.x2, f.y2, f.z2, sprite.getU(f.u2), sprite.getV(f.v2), r, g, b);
            vertex(bb, f.x3, f.y3, f.z3, sprite.getU(f.u3), sprite.getV(f.v3), r, g, b);
        }
        bb.setOffset(0, 0, 0);
        tess.draw();
    }
}
