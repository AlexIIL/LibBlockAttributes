/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Helper class for rendering {@link FluidVolume} instances. Block, Entity, and Screen renders should never call
 * instances of this class directly. Instead they should always use {@link FluidVolume#getRenderer()}. */
public abstract class FluidVolumeRenderer {

    /** Renders a list of faces of the given fluid at the specified co-ordinates. */
    public abstract void render(
        FluidVolume fluid, List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices
    );

    public void renderGuiRectangle(FluidVolume fluid, double x0, double y0, double x1, double y1) {
        List<FluidRenderFace> faces = new ArrayList<>();
        faces.add(FluidRenderFace.createFlatFaceZ(x0, y0, 0, x1, y1, 0, 1, true, false));

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        MatrixStack matrices = new MatrixStack();
        bb.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        render(fluid, faces, VertexConsumerProvider.immediate(bb), matrices);
        tess.draw();
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

    protected static RenderLayer getRenderLayer(FluidVolume fluid) {
        final RenderLayer layer;
        Fluid fl = fluid.getRawFluid();
        if (fl == null) {
            layer = RenderLayer.getTranslucent();
        } else {
            layer = RenderLayers.getFluidLayer(fl.getDefaultState());
        }
        return layer;
    }

    protected static Sprite[] getSprites(FluidVolume fluid) {
        final Sprite still;
        final Sprite flowing;
        Fluid fl = fluid.getRawFluid();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (fl != null) {
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fl);
            if (handler != null) {
                Sprite[] sprites = handler.getFluidSprites(null, null, fl.getDefaultState());
                assert sprites.length == 2;
                still = sprites[0];
                flowing = sprites[1];
            } else {
                BlockState state = fl.getDefaultState().getBlockState();
                still = mc.getBlockRenderManager().getModel(state).getSprite();
                flowing = still;
            }
        } else {
            SpriteAtlasTexture atlas = mc.getBakedModelManager().method_24153(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            // Seems odd that Sprite is AutoClosable... but there's nothing we can do about it
            still = atlas.getSprite(fluid.getStillSprite());
            flowing = atlas.getSprite(fluid.getFlowingSprite());
        }

        return new Sprite[] { still, flowing };
    }

    /** @deprecated Use {@link #renderSimpleFluid(List, VertexConsumer, MatrixStack, Sprite, Sprite, int)} instead,
     *             which takes both the still and flowing sprites. */
    @Deprecated
    protected static void renderSimpleFluid(
        List<FluidRenderFace> faces, VertexConsumer vc, MatrixStack matrices, Sprite sprite, int colour
    ) {
        renderSimpleFluid(faces, vc, matrices, sprite, sprite, colour);
    }

    protected static void renderSimpleFluid(
        List<FluidRenderFace> faces, VertexConsumer vc, MatrixStack matrices, Sprite still, Sprite flowing, int colour
    ) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = (colour >> 0) & 0xFF;

        Sprite _s = still;
        Sprite _f = flowing;
        for (FluidRenderFace f : splitFaces(faces)) {
            vertex(vc, matrices, f.x0, f.y0, f.z0, f.getU(_s, _f, f.u0), f.getV(_s, _f, f.v0), r, g, b);
            vertex(vc, matrices, f.x1, f.y1, f.z1, f.getU(_s, _f, f.u1), f.getV(_s, _f, f.v1), r, g, b);
            vertex(vc, matrices, f.x2, f.y2, f.z2, f.getU(_s, _f, f.u2), f.getV(_s, _f, f.v2), r, g, b);
            vertex(vc, matrices, f.x3, f.y3, f.z3, f.getU(_s, _f, f.u3), f.getV(_s, _f, f.v3), r, g, b);
        }
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_COLOR_TEXTURE} format.
     * 
     * @param vc The {@link VertexConsumer} to append to.
     * @param matrices The {@link MatrixStack} to apply to the x,y,z positions.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -> 1)
     * @param v Texture - V (0 -> 1)
     * @param r Colour - Red (0 -> 255)
     * @param g Colour - Green (0 -> 255)
     * @param b Colour - Blue (0 -> 255) */
    protected static void vertex(
        VertexConsumer vc, MatrixStack matrices, double x, double y, double z, float u, float v, int r, int g, int b
    ) {
        vc.vertex(matrices.peek().getModel(), (float) x, (float) y, (float) z);
        vc.color(r, g, b, 0xFF);
        vc.texture(u, v);
        vc.next();
    }

    public static final class ComponentRenderFaces {
        public final List<FluidRenderFace> split, splitExceptTextures;

        public ComponentRenderFaces(List<FluidRenderFace> split, List<FluidRenderFace> splitExceptTextures) {
            this.split = split;
            this.splitExceptTextures = splitExceptTextures;
        }
    }
}
