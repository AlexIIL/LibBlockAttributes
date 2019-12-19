/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/** A fluid that changes it's makup based on the {@link Biome}s that it is taken from. */
public class BiomeSourcedFluidVolume extends WeightedFluidVolume<Biome> {

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome value, FluidAmount amount) {
        super(key, value, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, Biome value, int amount) {
        super(key, value, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, FluidAmount amount) {
        super(key, Biomes.DEFAULT, amount);
    }

    @Deprecated
    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, int amount) {
        super(key, Biomes.DEFAULT, amount);
    }

    protected BiomeSourcedFluidVolume(BiomeSourcedFluidKey key, CompoundTag tag) {
        super(key, tag);
    }

    @Override
    public BiomeSourcedFluidKey getFluidKey() {
        return (BiomeSourcedFluidKey) this.fluidKey;
    }

    @Override
    protected String saveName() {
        return "biomes";
    }

    @Override
    protected Biome readValue(CompoundTag holder) {
        return Registry.BIOME.get(Identifier.tryParse(holder.getString("Name")));
    }

    @Override
    protected void writeValue(CompoundTag holder, Biome value) {
        Identifier id = Registry.BIOME.getId(value);
        if (id != null) {
            holder.putString("Name", id.toString());
        }
    }
}
