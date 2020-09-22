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

import net.minecraft.potion.Potions;

import alexiil.mc.lib.attributes.VanillaSetupBaseTester;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;

public class FluidUnitSetTester extends VanillaSetupBaseTester {

    @Test
    public void testWaterUnits() {

        boolean[] bools = { false, true };
        for (FluidKey fluid : new FluidKey[] { FluidKeys.WATER, FluidKeys.LAVA, FluidKeys.get(Potions.HEALING) }) {
            for (int flags = 0; flags <= 2 * 2 * 2; flags++) {
                boolean useSymbols = (flags & 1) == 1;
                boolean useShortDesc = (flags & 2) == 2;
                boolean joinName = (flags & 4) == 4;

                FluidTooltipContext ctx = FluidTooltipContext.USE_CONFIG//
                    .forceSymbols(useSymbols)//
                    .forceShortDesc(useShortDesc)//
                    .forceJoinedName(joinName);

                System.out.println(
                    "\n" + fluid + (useSymbols ? " symbols" : "") + (useShortDesc ? " short-desc" : "")
                        + (joinName ? " joined-name" : "")
                );

                int b = FluidVolume.BOTTLE;
                System.out.println(fluid.withAmount(1 * b).localizeAmount(ctx));
                System.out.println(fluid.withAmount(2 * b).localizeAmount(ctx));
                System.out.println(fluid.withAmount(3 * b).localizeAmount(ctx));
                System.out.println(fluid.withAmount(4 * b).localizeAmount(ctx));
                System.out.println(fluid.withAmount(5 * b).localizeAmount(ctx));
                System.out.println(fluid.withAmount(5 * b + 23).localizeAmount(ctx));
                if (fluid == FluidKeys.WATER) {
                    System.out.println(FluidUnit.BUCKET.localizeEmptyTank(FluidAmount.BUCKET, ctx));
                    System.out.println(FluidUnit.BUCKET.localizeFullTank(FluidAmount.BUCKET, ctx));
                    System.out
                        .println(FluidUnit.BUCKET.localizeTank(FluidAmount.BUCKET, FluidAmount.BUCKET.mul(2), ctx));
                }
            }
        }
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
