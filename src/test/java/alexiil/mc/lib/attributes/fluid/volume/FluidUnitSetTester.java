/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import org.junit.Assert;
import org.junit.Test;

import alexiil.mc.lib.attributes.VanillaSetupBaseTester;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public class FluidUnitSetTester extends VanillaSetupBaseTester {

    @Test
    public void testWaterUnits() {
        int b = FluidVolume.BOTTLE;
        System.out.println(FluidKeys.WATER.withAmount(b).localizeAmount());
        System.out.println(FluidKeys.WATER.withAmount(2 * b).localizeAmount());
        System.out.println(FluidKeys.WATER.withAmount(3 * b).localizeAmount());
        System.out.println(FluidKeys.WATER.withAmount(4 * b).localizeAmount());
        System.out.println(FluidKeys.WATER.withAmount(5 * b).localizeAmount());
        System.out.println(FluidKeys.WATER.withAmount(5 * b + 23).localizeAmount());
        System.out.println(FluidUnit.BUCKET.localizeEmptyTank(FluidAmount.BUCKET));
        System.out.println(FluidUnit.BUCKET.localizeFullTank(FluidAmount.BUCKET));
        System.out.println(FluidUnit.BUCKET.localizeTank(FluidAmount.BUCKET, FluidAmount.BUCKET.mul(2)));
    }

    @Test
    public void testIronUnits() {
        FluidUnit block = new FluidUnit(FluidVolume.BUCKET, "block");
        FluidUnit ingot = new FluidUnit(FluidVolume.BUCKET / 9, "ingot");
        FluidUnit nugget = new FluidUnit(FluidVolume.BUCKET / 9 / 9, "nugget");

        FluidUnitSet set = new FluidUnitSet();
        set.addUnit(block);
        set.addUnit(ingot);
        set.addUnit(nugget);

        Assert.assertEquals(block, set.getLargestUnit());
        Assert.assertEquals(nugget, set.getSmallestUnit());

        System.out.println(set.localizeAmount(FluidAmount.of(1, 6, 9).add(FluidAmount.of(4, 81))));
    }
}
