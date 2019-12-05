/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.Fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Default {@link FluidVolumeRenderer} that can handle most default fluid types - if the {@link FluidVolume} is for a
 * non-null minecraft {@link Fluid} then this will use the sprites provided from the {@link FluidRenderHandlerRegistry}.
 * Otherwise this will fallback to the sprites in {@link FluidVolume#getStillSprite()} and
 * {@link FluidVolume#getFlowingSprite()}. */
public class DefaultFluidVolumeRenderer extends FluidVolumeRenderer {

    public static final DefaultFluidVolumeRenderer INSTANCE = new DefaultFluidVolumeRenderer();

    protected DefaultFluidVolumeRenderer() {}

    @Override
    public void render(
        FluidVolume fluid, List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices
    ) {
        Sprite[] sprites = getSprites(fluid);
        RenderLayer layer = getRenderLayer(fluid);
        renderSimpleFluid(faces, vcp.getBuffer(layer), matrices, sprites[0], sprites[1], fluid.getRenderColor());
    }
}
