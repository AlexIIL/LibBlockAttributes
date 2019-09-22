/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

/* package-private */ final class WaterFluidKey extends BiomeSourcedFluidKey {

    static final WaterFluidKey INSTANCE = new WaterFluidKey();

    private WaterFluidKey() {
        super(NormalFluidKey.builder(Fluids.WATER, //
            new Identifier("minecraft", "block/water_still"), //
            new TranslatableText("block.minecraft.water")//
        ).addUnit(FluidUnit.BOTTLE));
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new WaterFluidVolume(tag);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(int amount) {
        return new WaterFluidVolume(amount);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(Biome source, int amount) {
        return new WaterFluidVolume(source, amount);
    }
}
