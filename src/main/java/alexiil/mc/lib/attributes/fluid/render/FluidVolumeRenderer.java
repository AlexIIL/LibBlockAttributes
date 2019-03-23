package alexiil.mc.lib.attributes.fluid.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Helper class for rendering {@link FluidVolume} instances. Block, Entity, and Screen renders should never call
 * instances of this class directly. Instead they should always use {@link FluidVolume#getRenderer()}. */
public abstract class FluidVolumeRenderer {

    /** Renders a list of faces of the given fluid at the specified co-ordinates. */
    public abstract void render(FluidVolume fluid, List<FluidRenderFace> faces, double x, double y, double z);

    public void renderGuiRectangle(FluidVolume fluid, double x0, double y0, double x1, double y1) {
        List<FluidRenderFace> faces = new ArrayList<>();
        faces.add(FluidRenderFace.createFlatFaceZ(x0, y0, 0, x1, y1, 0, 1, true));
        render(fluid, faces, 0, 0, 30);
    }

    protected static List<FluidRenderFace> splitFaces(List<FluidRenderFace> faces) {
        return FluidFaceSplitter.splitFaces(faces);
    }

    /** Like {@link #splitFaces(List)} but also returns the
     * 
     * @param faces
     * @return */
    protected static ComponentRenderFaces splitFacesComponent(List<FluidRenderFace> faces) {
        return FluidFaceSplitter.splitFacesComponent(faces);
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_UV_COLOR} format.
     * 
     * @param bb The {@link BufferBuilder} to append to.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -> 1)
     * @param v Texture - V (0 -> 1)
     * @param r Colour - Red (0 -> 255)
     * @param g Colour - Green (0 -> 255)
     * @param b Colour - Blue (0 -> 255) */
    protected static void vertex(BufferBuilder bb, double x, double y, double z, float u, float v, int r, int g,
        int b) {
        bb.vertex(x, y, z);
        bb.texture(u, v);
        bb.color(r, g, b, 0xFF);
        bb.next();
    }

    public static final class ComponentRenderFaces {
        public final List<FluidRenderFace> split, splitExceptTextures;

        public ComponentRenderFaces(List<FluidRenderFace> split, List<FluidRenderFace> splitExceptTextures) {
            this.split = split;
            this.splitExceptTextures = splitExceptTextures;
        }
    }
}
