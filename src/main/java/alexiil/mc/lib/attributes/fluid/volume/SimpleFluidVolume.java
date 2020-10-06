/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import com.google.gson.JsonObject;

import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** Simple implementation of a {@link FluidVolume} that doesn't do anything special. */
public class SimpleFluidVolume extends FluidVolume {

    protected SimpleFluidVolume(SimpleFluidKey key, FluidAmount amount) {
        super(key, amount);
    }

    @Deprecated // (since = "0.6.4", forRemoval = true)
    protected SimpleFluidVolume(SimpleFluidKey key, int amount) {
        super(key, amount);
    }

    protected SimpleFluidVolume(SimpleFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    protected SimpleFluidVolume(SimpleFluidKey key, JsonObject json) {
        super(key, json);
    }
}
