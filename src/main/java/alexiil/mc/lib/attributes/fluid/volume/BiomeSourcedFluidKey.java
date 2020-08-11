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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BuiltInBiomes;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import net.minecraft.class_5423;

public class BiomeSourcedFluidKey extends WeightedFluidKey<RegistryKey<Biome>> {

    private static final RegistryKey<Biome> DEFAULT_BIOME_ID = BuiltInBiomes.OCEAN;

    public BiomeSourcedFluidKey(FluidKeyBuilder builder) {
        super(builder, DEFAULT_BIOME_ID);
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new BiomeSourcedFluidVolume(this, tag);
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new BiomeSourcedFluidVolume(this, json);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(FluidAmount amount) {
        return new BiomeSourcedFluidVolume(this, amount);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(RegistryKey<Biome> source, FluidAmount amount) {
        return new BiomeSourcedFluidVolume(this, source, amount);
    }

    @Override
    public FluidVolume fromWorld(class_5423 world, BlockPos pos) {
        RegistryKey<Biome> biomeId = world.method_31081(pos).orElse(DEFAULT_BIOME_ID);

        return withAmount(biomeId, FluidAmount.BUCKET);
    }
}
