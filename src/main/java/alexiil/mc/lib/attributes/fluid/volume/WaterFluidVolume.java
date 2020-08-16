/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public final class WaterFluidVolume extends BiomeSourcedFluidVolume {

    public WaterFluidVolume(FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, Biomes.OCEAN, amount);
    }

    @Deprecated
    public WaterFluidVolume(int amount) {
        super(WaterFluidKey.INSTANCE, Biomes.OCEAN, amount);
    }

    public WaterFluidVolume(Biome source, FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    @Deprecated
    public WaterFluidVolume(Biome source, int amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    public WaterFluidVolume(CompoundTag tag) {
        super(WaterFluidKey.INSTANCE, tag);
    }

    @Override
    public int getRenderColor() {
        Map<Biome, FluidAmount> sources = this.getValues();
        int biomeCount = sources.size();
        switch (biomeCount) {
            case 0: {
                // Um, what?
                return Biomes.DEFAULT.getWaterColor();
            }
            case 1: {
                return sources.keySet().iterator().next().getWaterColor();
            }
            default: {
                double r = 0;
                double g = 0;
                double b = 0;
                double total = 0;

                for (Biome biome : sources.keySet()) {
                    FluidAmount flAmount = sources.get(biome);
                    double amount = flAmount.asInexactDouble();
                    int colour = biome.getWaterColor();
                    r += (colour & 0xFF) * amount;
                    g += ((colour >> 8) & 0xFF) * amount;
                    b += ((colour >> 16) & 0xFF) * amount;
                    total += amount;
                }

                r /= total;
                g /= total;
                b /= total;

                assert r >= 0;
                assert g >= 0;
                assert b >= 0;

                assert r < 256;
                assert g < 256;
                assert b < 256;

                return ((int) r) | ((int) g << 8) | ((int) b << 16);
            }
        }
    }
}
