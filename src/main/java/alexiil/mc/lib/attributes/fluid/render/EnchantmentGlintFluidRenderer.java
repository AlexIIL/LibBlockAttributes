/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class EnchantmentGlintFluidRenderer extends FluidVolumeRenderer {

    public static final EnchantmentGlintFluidRenderer INSTANCE = new EnchantmentGlintFluidRenderer();

    protected EnchantmentGlintFluidRenderer() {}

    @Override
    public void render(
        FluidVolume fluid, List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices
    ) {
        Sprite[] sprites = getSprites(fluid);
        RenderLayer layer = getRenderLayer(fluid);
        VertexConsumer vc = ItemRenderer.getItemGlintConsumer(vcp, layer, true, true);
        renderSimpleFluid(faces, vc, matrices, sprites[0], sprites[1], fluid.getRenderColor());
    }
}
