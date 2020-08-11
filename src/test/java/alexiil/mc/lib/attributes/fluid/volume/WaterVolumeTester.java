/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import net.minecraft.world.biome.BuiltInBiomes;
import org.junit.Assert;

import net.minecraft.world.biome.Biomes;

import alexiil.mc.lib.attributes.Simulation;

public class WaterVolumeTester {

    public static void runTests() {
        testWaterVolumeSplitting();
        testWaterVolumeSplittingSmall();
    }

    private static void testWaterVolumeSplitting() {
        WaterFluidVolume waterVolume = new WaterFluidVolume(BuiltInBiomes.OCEAN, 10);
        Assert.assertTrue(waterVolume.merge(new WaterFluidVolume(BuiltInBiomes.DARK_FOREST, 10), Simulation.ACTION));

        FluidVolume split1 = waterVolume.split(1);
        FluidVolume split2 = waterVolume.split(2);
        FluidVolume split3 = waterVolume.split(3);
        FluidVolume split4 = waterVolume.split(4);
        FluidVolume split5 = waterVolume.split(5);

        Assert.assertEquals(1, split1.getAmount());
        Assert.assertEquals(2, split2.getAmount());
        Assert.assertEquals(3, split3.getAmount());
        Assert.assertEquals(4, split4.getAmount());
        Assert.assertEquals(5, split5.getAmount());
        Assert.assertEquals(5, waterVolume.getAmount());
    }

    private static void testWaterVolumeSplittingSmall() {
        WaterFluidVolume waterVolume = new WaterFluidVolume(BuiltInBiomes.OCEAN, 1);
        Assert.assertTrue(waterVolume.merge(new WaterFluidVolume(BuiltInBiomes.DARK_FOREST, 1), Simulation.ACTION));

        FluidVolume split1 = waterVolume.split(1);

        Assert.assertEquals(1, split1.getAmount());
        Assert.assertEquals(1, waterVolume.getAmount());
    }
}
