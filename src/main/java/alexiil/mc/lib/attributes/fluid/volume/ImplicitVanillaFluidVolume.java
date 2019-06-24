/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.render.FluidVolumeRenderer;
import alexiil.mc.lib.attributes.fluid.render.ImplicitVanillaFluidVolumeRenderer;

final class ImplicitVanillaFluidVolume extends NormalFluidVolume {

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, int amount) {
        super(fluid, amount);
    }

    ImplicitVanillaFluidVolume(ImplicitVanillaFluidKey fluid, CompoundTag tag) {
        super(fluid, tag);
    }

    @Override
    public FluidVolumeRenderer getRenderer() {
        return ImplicitVanillaFluidVolumeRenderer.INSTANCE;
    }
}
