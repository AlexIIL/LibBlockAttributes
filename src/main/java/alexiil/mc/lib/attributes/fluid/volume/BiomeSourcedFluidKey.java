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
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.mixin.impl.BiomeEffectsAccessor;

/** A {@link ColouredFluidKey} which gets it's colour from a {@link Biome}s water colour. */
public class BiomeSourcedFluidKey extends ColouredFluidKey {

    @Deprecated
    public BiomeSourcedFluidKey(FluidKeyBuilder builder) {
        this(new ColouredFluidKeyBuilder().copyFrom(builder));
    }

    public BiomeSourcedFluidKey(ColouredFluidKeyBuilder builder) {
        super(builder);
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

    public BiomeSourcedFluidVolume withAmount(Biome source, FluidAmount amount) {
        BiomeSourcedFluidVolume volume = new BiomeSourcedFluidVolume(this, amount);
        volume.setColourFromBiome(source);
        return volume;
    }

    public int getColourFromBiome(Biome source) {
        return 0xFF_00_00_00 | ((BiomeEffectsAccessor) source.getEffects()).libblockattributes_getWaterColour();
    }

    @Override
    public FluidVolume fromWorld(WorldView world, BlockPos pos) {
        return withAmount(world.getBiome(pos), FluidAmount.BUCKET);
    }
}
