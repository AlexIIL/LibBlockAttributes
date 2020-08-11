/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BuiltInBiomes;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

/* package-private */ final class WaterFluidVolume extends BiomeSourcedFluidVolume {

    public WaterFluidVolume(FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, BuiltInBiomes.OCEAN, amount);
    }

    @Deprecated
    public WaterFluidVolume(int amount) {
        super(WaterFluidKey.INSTANCE, BuiltInBiomes.OCEAN, amount);
    }

    public WaterFluidVolume(RegistryKey<Biome> source, FluidAmount amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    @Deprecated
    public WaterFluidVolume(RegistryKey<Biome> source, int amount) {
        super(WaterFluidKey.INSTANCE, source, amount);
    }

    public WaterFluidVolume(CompoundTag tag) {
        super(WaterFluidKey.INSTANCE, tag);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public int getRenderColor() {
        ClientWorld world = MinecraftClient.getInstance().world;
        // We need a registry manager...
        if (world == null) {
            return super.getRenderColor(); // Use the fluid default
        }

        Registry<Biome> biomes = world.getRegistryManager().get(Registry.BIOME_KEY);

        Map<RegistryKey<Biome>, FluidAmount> sources = this.getValues();
        int biomeCount = sources.size();
        switch (biomeCount) {
            case 0: {
                // Um, what?
                Biome biome = biomes.get(BuiltInBiomes.PLAINS);
                if (biome == null) {
                    return super.getRenderColor();
                }
                return biome.getWaterColor();
            }
            case 1: {
                Biome biome = biomes.get(sources.keySet().iterator().next());
                if (biome == null) {
                    return super.getRenderColor();
                }
                return biome.getWaterColor();
            }
            default: {
                double r = 0;
                double g = 0;
                double b = 0;
                double total = 0;

                for (RegistryKey<Biome> biomeId : sources.keySet()) {
                    FluidAmount flAmount = sources.get(biomeId);
                    double amount = flAmount.asInexactDouble();

                    Biome biome = biomes.get(biomeId);
                    int colour;
                    if (biome == null) {
                        colour = super.getRenderColor();
                    } else {
                        colour = biome.getWaterColor();
                    }
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
