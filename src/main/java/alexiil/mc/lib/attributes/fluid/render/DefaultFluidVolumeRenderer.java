/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class DefaultFluidVolumeRenderer extends FluidVolumeRenderer {

    public static final DefaultFluidVolumeRenderer INSTANCE = new DefaultFluidVolumeRenderer();

    protected DefaultFluidVolumeRenderer() {}

    @Override
    public void render(FluidVolume fluid, List<FluidRenderFace> faces, double x, double y, double z) {
        Sprite sprite = MinecraftClient.getInstance().getSpriteAtlas().getSprite(fluid.getSprite());
        int colour = fluid.getRenderColor();
        renderSimpleFluid(faces, x, y, z, sprite, colour);
    }
}
