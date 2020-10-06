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
import net.minecraft.world.biome.Biome;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/* package-private */ final class WaterFluidVolume extends BiomeSourcedFluidVolume {

    public WaterFluidVolume(FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, amount);
    }

    @Deprecated // (since = "0.6.4", forRemoval = true)
    public WaterFluidVolume(int amount) {
        super(WaterFluidKey.INSTANCE, amount);
    }

    public WaterFluidVolume(Biome source, FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    @Deprecated // (since = "0.6.4", forRemoval = true)
    public WaterFluidVolume(Biome source, int amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    public WaterFluidVolume(CompoundTag tag) {
        super(WaterFluidKey.INSTANCE, tag);
    }

    public WaterFluidVolume(JsonObject json) {
        super(WaterFluidKey.INSTANCE, json);
    }
}
