/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public class BiomeSourcedFluidKey extends WeightedFluidKey<Biome> {
    public BiomeSourcedFluidKey(FluidKeyBuilder builder) {
        super(builder, Biome.class, Biomes.OCEAN);
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new BiomeSourcedFluidVolume(this, tag);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(FluidAmount amount) {
        return new BiomeSourcedFluidVolume(this, amount);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(Biome source, FluidAmount amount) {
        return new BiomeSourcedFluidVolume(this, source, amount);
    }

    @Override
    public FluidVolume fromWorld(WorldView world, BlockPos pos) {
        return withAmount(world.getBiome(pos), FluidAmount.BUCKET);
    }
}
