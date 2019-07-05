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
import net.minecraft.world.ViewableWorld;
import net.minecraft.world.biome.Biome;

public class BiomeSourcedFluidKey extends NormalFluidKey {
    public BiomeSourcedFluidKey(NormalFluidKeyBuilder builder) {
        super(builder);
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new BiomeSourcedFluidVolume(this, tag);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(int amount) {
        return new BiomeSourcedFluidVolume(this, amount);
    }

    public BiomeSourcedFluidVolume withAmount(Biome source, int amount) {
        return new BiomeSourcedFluidVolume(this, source, amount);
    }

    @Override
    public FluidVolume fromWorld(ViewableWorld world, BlockPos pos) {
        return withAmount(world.getBiome(pos), FluidVolume.BUCKET);
    }
}
