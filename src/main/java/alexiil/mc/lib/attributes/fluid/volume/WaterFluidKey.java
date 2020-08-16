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

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public final class WaterFluidKey extends BiomeSourcedFluidKey {

    public static final Identifier SPRITE_STILL = new Identifier("minecraft", "block/water_still");
    public static final Identifier SPRITE_FLOWING = new Identifier("minecraft", "block/water_flowing");

    static final WaterFluidKey INSTANCE = new WaterFluidKey();

    private WaterFluidKey() {
        super(
            new FluidKeyBuilder(Fluids.WATER)//
                .setSprites(SPRITE_STILL, SPRITE_FLOWING)//
                .setName(new TranslatableText("block.minecraft.water"))//
                .addUnit(FluidUnit.BOTTLE)
        );
    }

    @Override
    public BiomeSourcedFluidVolume readVolume(CompoundTag tag) {
        return new WaterFluidVolume(tag);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(FluidAmount amount) {
        return new WaterFluidVolume(amount);
    }

    @Override
    public BiomeSourcedFluidVolume withAmount(Biome source, FluidAmount amount) {
        return new WaterFluidVolume(source, amount);
    }
}
