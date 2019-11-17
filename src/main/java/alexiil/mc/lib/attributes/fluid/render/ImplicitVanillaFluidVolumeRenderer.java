/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.fluid.Fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.fluid.volume.NormalFluidVolume;

public class ImplicitVanillaFluidVolumeRenderer extends FluidVolumeRenderer {

    public static final ImplicitVanillaFluidVolumeRenderer INSTANCE = new ImplicitVanillaFluidVolumeRenderer();

    @Override
    public void render(FluidVolume fluid, List<FluidRenderFace> faces, double x, double y, double z) {
        int colour = fluid.getRenderColor();
        final Sprite still, flowing;

        if (fluid instanceof NormalFluidVolume) {
            Fluid fl = ((NormalFluidVolume) fluid).getRawFluid();
            FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fl);
            if (handler != null) {
                Sprite[] sprites = handler.getFluidSprites(null, null, fl.getDefaultState());
                assert sprites.length == 2;
                still = sprites[0];
                flowing = sprites[1];
            } else {
                BlockState state = fl.getDefaultState().getBlockState();
                still = MinecraftClient.getInstance().getBlockRenderManager().getModel(state).getSprite();
                flowing = still;
            }
        } else {
            SpriteAtlasTexture atlas = MinecraftClient.getInstance().getSpriteAtlas();
            still = atlas.getSprite(fluid.getStillSprite());
            flowing = atlas.getSprite(fluid.getFlowingSprite());
        }

        renderSimpleFluid(faces, x, y, z, still, flowing, colour);
    }
}
