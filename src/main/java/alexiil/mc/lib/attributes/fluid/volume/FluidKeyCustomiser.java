/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluid;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey.FluidKeyBuilder;

/** Interface for minecraft {@link Fluid} to implement to customise the implicit {@link FluidKey} that gets created for
 * them. Note that this is not a replacement for registering a {@link FluidKey} yourself: this only supports customising
 * {@link SimpleFluidKey}. */
public interface FluidKeyCustomiser {

    /** Customises the {@link SimpleFluidKey} that is created implicitly for this {@link Fluid}. */
    void customiseKey(FluidKeyBuilder builder);
}
