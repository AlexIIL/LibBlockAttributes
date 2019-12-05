/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.render;

import java.util.List;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** @deprecated Because {@link DefaultFluidVolumeRenderer} does everything that this does. */
@Deprecated
public class ImplicitVanillaFluidVolumeRenderer extends FluidVolumeRenderer {

    public static final ImplicitVanillaFluidVolumeRenderer INSTANCE = new ImplicitVanillaFluidVolumeRenderer();

    @Override
    public void render(
        FluidVolume fluid, List<FluidRenderFace> faces, VertexConsumerProvider vcp, MatrixStack matrices
    ) {
        DefaultFluidVolumeRenderer.INSTANCE.render(fluid, faces, vcp, matrices);
    }
}
