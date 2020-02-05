/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.nbt.CompoundTag;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** Simple implementation of a {@link FluidKey} that doesn't do anything special. */
public class SimpleFluidKey extends FluidKey {

    public SimpleFluidKey(FluidKeyBuilder builder) {
        super(builder);
    }

    @Override
    public FluidVolume readVolume(CompoundTag tag) {
        return new SimpleFluidVolume(this, tag);
    }

    @Override
    public FluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new SimpleFluidVolume(this, json);
    }

    @Override
    public FluidVolume withAmount(FluidAmount amount) {
        return new SimpleFluidVolume(this, amount);
    }
}
