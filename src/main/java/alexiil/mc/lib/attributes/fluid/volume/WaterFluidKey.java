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
            ((ColouredFluidKeyBuilder) new ColouredFluidKeyBuilder(Fluids.WATER)//
                .setSprites(SPRITE_STILL, SPRITE_FLOWING)//
                .setName(new TranslatableText("block.minecraft.water"))//
                .addUnit(FluidUnit.BOTTLE)//
            )// end of FluidKeyBuilder
                .setAlphaBounds(0.25f, 1)//
                .setDefaultColour(0x3f / 255f, 0x76 / 255f, 0xE4 / 255f, 1)//
        );
    }

    @Override
    public WaterFluidVolume readVolume(CompoundTag tag) {
        return new WaterFluidVolume(tag);
    }

    @Override
    public WaterFluidVolume withAmount(FluidAmount amount) {
        return new WaterFluidVolume(amount);
    }

    @Override
    public WaterFluidVolume withAmount(Biome source, FluidAmount amount) {
        return new WaterFluidVolume(source, amount);
    }

    @Override
    public WaterFluidVolume readVolume(JsonObject json) throws JsonSyntaxException {
        return new WaterFluidVolume(json);
    }
}
