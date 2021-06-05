/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import com.google.gson.JsonObject;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.biome.Biome;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** A fluid that changes it's makup based on the {@link Biome}s that it is taken from. */
public class BiomeSourcedFluidVolume extends ColouredFluidVolume {

    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome source, FluidAmount amount) {
        super(key, amount);
        setColourFromBiome(source);
    }

    @Deprecated(since = "0.6.4", forRemoval = true)
    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome source, int amount) {
        super(key, amount);
        setColourFromBiome(source);
    }

    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, FluidAmount amount) {
        super(key, amount);
    }

    @Deprecated(since = "0.6.4", forRemoval = true)
    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, int amount) {
        super(key, amount);
    }

    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, NbtCompound tag) {
        super(key, tag);
    }

    public BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, JsonObject json) {
        super(key, json);
    }

    @Override
    public BiomeSourcedFluidKey getFluidKey() {
        return (BiomeSourcedFluidKey) this.fluidKey;
    }

    public void setColourFromBiome(Biome source) {
        setArgb(getFluidKey().getColourFromBiome(source));
    }
}
