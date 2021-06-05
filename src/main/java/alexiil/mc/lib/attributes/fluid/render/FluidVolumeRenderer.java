/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluid;
import net.minecraft.screen.PlayerScreenHandler;

import alexiil.mc.lib.attributes.fluid.mixin.impl.RenderLayerAccessor;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Helper class for rendering {@link FluidVolume} instances. Block, Entity, and Screen renders should never call
 * instances of this class directly. Instead they should always use {@link FluidVolume#getRenderer()}. */
public abstract class FluidVolumeRenderer {
    public static final ExpandingVcp VCPS = new ExpandingVcp();

    /** Renders a list of faces of the given fluid at the specified co-ordinates. */
    public abstract void render(
        FluidVolume fluid, List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices
    );

    public void renderGuiRectangle(FluidVolume fluid, double x0, double y0, double x1, double y1) {
        List<FluidRenderFace> faces = new ArrayList<>();
        faces.add(FluidRenderFace.createFlatFaceZ(0, 0, 0, x1 - x0, y1 - y0, 0, 1 / 16.0, false, false));

        MatrixStack matrices = new MatrixStack();
        matrices.translate(x0, y0, 0);
        render(fluid, faces, VCPS, matrices);
        VCPS.draw();
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
            SpriteAtlasTexture atlas = mc.getBakedModelManager().getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            // Seems odd that Sprite is AutoClosable... but there's nothing we can do about it
            still = atlas.getSprite(fluid.getStillSprite());
            flowing = atlas.getSprite(fluid.getFlowingSprite());
        }

        return new Sprite[] { still, flowing };
    }

    /** @deprecated Use {@link #renderSimpleFluid(List, VertexConsumer, MatrixStack, Sprite, Sprite, int)} instead,
     *             which takes both the still and flowing sprites. */
    @Deprecated(since = "0.6.0", forRemoval = true)
    protected static void renderSimpleFluid(
        List<FluidRenderFace> faces, VertexConsumer vc, MatrixStack matrices, Sprite sprite, int colour
    ) {
        renderSimpleFluid(faces, vc, matrices, sprite, sprite, colour);
    }

    /** Renders a fluid in {@link VertexFormats#POSITION_COLOR_TEXTURE} */
    protected static void renderSimpleFluid(
        List<FluidRenderFace> faces, VertexConsumer vc, MatrixStack matrices, Sprite still, Sprite flowing, int colour
    ) {
        int a = (colour >>> 24) & 0xFF;
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = (colour >> 0) & 0xFF;

        Sprite _s = still;
        Sprite _f = flowing;
        for (FluidRenderFace f : splitFaces(faces)) {
            vertex(vc, matrices, f.x0, f.y0, f.z0, f.getU(_s, _f, f.u0), f.getV(_s, _f, f.v0), r, g, b, a, f);
            vertex(vc, matrices, f.x1, f.y1, f.z1, f.getU(_s, _f, f.u1), f.getV(_s, _f, f.v1), r, g, b, a, f);
            vertex(vc, matrices, f.x2, f.y2, f.z2, f.getU(_s, _f, f.u2), f.getV(_s, _f, f.v2), r, g, b, a, f);
            vertex(vc, matrices, f.x3, f.y3, f.z3, f.getU(_s, _f, f.u3), f.getV(_s, _f, f.v3), r, g, b, a, f);
        }
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_COLOR_TEXTURE_LIGHT_NORMAL} format.
     * 
     * @param vc The {@link VertexConsumer} to append to.
     * @param matrices The {@link MatrixStack} to apply to the x,y,z positions.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -&gt; 1)
     * @param v Texture - V (0 -&gt; 1)
     * @param r Colour - Red (0 -&gt; 255)
     * @param g Colour - Green (0 -&gt; 255)
     * @param b Colour - Blue (0 -&gt; 255)
     * @param f The source for the light and normal. */
    protected static void vertex(
        VertexConsumer vc, MatrixStack matrices, double x, double y, double z, float u, float v, int r, int g, int b,
        FluidRenderFace f
    ) {
        vertex(vc, matrices, x, y, z, u, v, r, g, b, 0xFF, f);
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_COLOR_TEXTURE_LIGHT_NORMAL} format.
     * 
     * @param vc The {@link VertexConsumer} to append to.
     * @param matrices The {@link MatrixStack} to apply to the x,y,z positions.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -&gt; 1)
     * @param v Texture - V (0 -&gt; 1)
     * @param r Colour - Red (0 -&gt; 255)
     * @param g Colour - Green (0 -&gt; 255)
     * @param b Colour - Blue (0 -&gt; 255)
     * @param a Colour - Alpha (0 -&gt; 255)
     * @param f The source for the light and normal. */
    protected static void vertex(
        VertexConsumer vc, MatrixStack matrices, double x, double y, double z, float u, float v, int r, int g, int b,
        int a, FluidRenderFace f
    ) {
        vertex(vc, matrices, x, y, z, u, v, r, g, b, a, f.light, f.nx, f.ny, f.nz);
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_COLOR_TEXTURE_LIGHT_NORMAL} format.
     * 
     * @param vc The {@link VertexConsumer} to append to.
     * @param matrices The {@link MatrixStack} to apply to the x,y,z positions.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -&gt; 1)
     * @param v Texture - V (0 -&gt; 1)
     * @param r Colour - Red (0 -&gt; 255)
     * @param g Colour - Green (0 -&gt; 255)
     * @param b Colour - Blue (0 -&gt; 255)
     * @param light Light - packed.
     * @param nx Normal - X
     * @param ny Normal - Y
     * @param nz Normal - Z */
    protected static void vertex(
        VertexConsumer vc, MatrixStack matrices, double x, double y, double z, float u, float v, int r, int g, int b,
        int light, float nx, float ny, float nz
    ) {
        vertex(vc, matrices, x, y, z, u, v, r, g, b, 0xFF, light, nx, ny, nz);
    }

    /** Appends a single vertex in {@link VertexFormats#POSITION_COLOR_TEXTURE_LIGHT_NORMAL} format.
     * 
     * @param vc The {@link VertexConsumer} to append to.
     * @param matrices The {@link MatrixStack} to apply to the x,y,z positions.
     * @param x Position - X
     * @param y Position - Y
     * @param z Position - Z
     * @param u Texture - U (0 -&gt; 1)
     * @param v Texture - V (0 -&gt; 1)
     * @param r Colour - Red (0 -&gt; 255)
     * @param g Colour - Green (0 -&gt; 255)
     * @param b Colour - Blue (0 -&gt; 255)
     * @param a Colour - Alpha (0 -&gt; 255)
     * @param light Light - packed.
     * @param nx Normal - X
     * @param ny Normal - Y
     * @param nz Normal - Z */
    protected static void vertex(
        VertexConsumer vc, MatrixStack matrices, double x, double y, double z, float u, float v, int r, int g, int b,
        int a, int light, float nx, float ny, float nz
    ) {
        vc.vertex(matrices.peek().getModel(), (float) x, (float) y, (float) z);
        vc.color(r, g, b, a == 0 ? 0xFF : a);
        vc.texture(u, v);
        vc.overlay(OverlayTexture.DEFAULT_UV);
        vc.light(light);
        vc.normal(matrices.peek().getNormal(), nx, ny, nz);
        vc.next();
    }

    public static final class ComponentRenderFaces {
        public final List<FluidRenderFace> split, splitExceptTextures;

        public ComponentRenderFaces(List<FluidRenderFace> split, List<FluidRenderFace> splitExceptTextures) {
            this.split = split;
            this.splitExceptTextures = splitExceptTextures;
        }
    }

    /** A simple, auto-expanding {@link VertexConsumerProvider} that can render any number of {@link RenderLayer}'s at
     * once, rather than {@link net.minecraft.client.render.VertexConsumerProvider.Immediate
     * VertexConsumerProvider.Immediate} which can only render the ones provided to it in a map, and 1 other. */
    public static final class ExpandingVcp implements VertexConsumerProvider {
        private final List<RenderLayer> before = new ArrayList<>();
        private final List<RenderLayer> solid = new ArrayList<>();
        private final List<RenderLayer> middle = new ArrayList<>();
        private final List<RenderLayer> translucent = new ArrayList<>();
        private final List<RenderLayer> after = new ArrayList<>();

        private final List<BufferBuilder> availableBuffers = new ArrayList<>();
        private final Map<RenderLayer, BufferBuilder> activeBuffers = new HashMap<>();
        private final Set<RenderLayer> knownLayers = new HashSet<>();

        public ExpandingVcp() {
            addLayer(RenderLayer.getSolid());
            addLayer(RenderLayer.getCutout());
            addLayer(RenderLayer.getCutoutMipped());
            addLayer(RenderLayer.getTranslucent());
            addLayer(RenderLayer.getTranslucentNoCrumbling());
            addLayerAfter(RenderLayer.getGlint());
            addLayerAfter(RenderLayer.getEntityGlint());
        }

        public void addLayer(RenderLayer layer) {
            if (knownLayers.add(layer)) {
                if (((RenderLayerAccessor) layer).libblockattributes_isTranslucent()) {
                    translucent.add(layer);
                } else {
                    solid.add(layer);
                }
            }
        }

        public void addLayerBefore(RenderLayer layer) {
            if (knownLayers.add(layer)) {
                before.add(layer);
            }
        }

        public void addLayerMiddle(RenderLayer layer) {
            if (knownLayers.add(layer)) {
                middle.add(layer);
            }
        }

        public void addLayerAfter(RenderLayer layer) {
            if (knownLayers.add(layer)) {
                after.add(layer);
            }
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            addLayer(layer);
            BufferBuilder buffer = activeBuffers.get(layer);
            if (buffer == null) {
                if (availableBuffers.isEmpty()) {
                    buffer = new BufferBuilder(1 << 12);
                } else {
                    buffer = availableBuffers.remove(availableBuffers.size() - 1);
                }
                activeBuffers.put(layer, buffer);
            }
            if (!buffer.isBuilding()) {
                buffer.begin(layer.getDrawMode(), layer.getVertexFormat());
            }
            return buffer;
        }

        /** Draws every buffer in this VCP, explicitly not using the {@link GraphicsMode#FABULOUS} mode's alternate
         * framebuffer, so this is safe to use in GUIs.
         * 
         * @see #drawDirectly() */
        public void draw() {
            RenderSystem.runAsFancy(this::drawDirectly);
        }

        /** Directly draws every buffer in this VCP. NOTE: in GUIs this won't work correctly when
         * {@link GraphicsMode#FABULOUS} is used: instead you should use {@link #draw()}. */
        public void drawDirectly() {
            draw(before);
            draw(solid);
            draw(middle);
            draw(translucent);
            draw(after);
            assert activeBuffers.isEmpty();
        }

        private void draw(List<RenderLayer> layers) {
            for (RenderLayer layer : layers) {
                BufferBuilder buffer = activeBuffers.remove(layer);
                if (buffer != null) {
                    layer.draw(buffer, 0, 0, 0);
                }
            }
        }
    }
}
