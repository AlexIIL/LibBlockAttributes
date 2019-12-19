/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.amount;

import org.junit.Assert;
import org.junit.Test;

public class FluidAmountTester {

    @Test
    public void testStrings() {
        System.out.println(FluidAmount.ZERO);
        System.out.println(FluidAmount.ONE);
        System.out.println(FluidAmount.BOTTLE);
        System.out.println(FluidAmount.of(1, 3));
    }

    @Test
    public void testEquality() {
        Assert.assertEquals(FluidAmount.ZERO, new FluidAmount(0));
        Assert.assertEquals(FluidAmount.ONE, new FluidAmount(1));
        Assert.assertEquals(FluidAmount.NEGATIVE_ONE, new FluidAmount(-1));
    }

    @Test
    public void testBalancing() {
        Assert.assertEquals(FluidAmount.of(1, 2), FluidAmount.of(2, 4));
        Assert.assertEquals(FluidAmount.of(1, 1), FluidAmount.ONE);
        Assert.assertEquals(FluidAmount.of(2, 2), FluidAmount.ONE);
        Assert.assertEquals(FluidAmount.of(1, 3, 4), FluidAmount.of(7, 4));
        Assert.assertEquals(FluidAmount.of(-1, -3, 4), FluidAmount.of(-7, 4));

        Assert.assertEquals(FluidAmount.of(1, 2), FluidAmount.of(1, -1, 2));
        Assert.assertEquals(FluidAmount.of(-4, -1, 2), FluidAmount.of(-5, 1, 2));
    }

    @Test
    public void testAdd() {
        FluidAmount half = FluidAmount.of(1, 2);
        Assert.assertEquals(FluidAmount.ONE, half.roundedAdd(half));
        Assert.assertEquals(FluidAmount.of(2, 1, 2), half.add(2));
    }

    @Test
    public void testMultiply() {
        FluidAmount half = FluidAmount.of(1, 2);
        Assert.assertEquals(FluidAmount.ONE, half.roundedMul(2));
        Assert.assertEquals(FluidAmount.of(1, 4), half.saturatedMul(half));

        // Check to see if it will overflow

        // 4 * (a-1)/a
        // (4a-4)/a
        // 4-4/a
        // 3+(a-4)/a
        long prettyBig = Long.MAX_VALUE / 2;
        FluidAmount expected = FluidAmount.of(3, prettyBig - 4, prettyBig);
        FluidAmount a = FluidAmount.of(prettyBig - 1, prettyBig);
        Assert.assertEquals(expected, a.checkedMul(4));
    }

    @Test
    public void testDiv() {
        FluidAmount half = FluidAmount.of(1, 2);
        Assert.assertEquals(FluidAmount.ONE, half.roundedDiv(half));
        Assert.assertEquals(FluidAmount.of(1, 4), half.roundedDiv(FluidAmount.ofWhole(2)));
    }

    @Test
    public void testParseLongs() {
        Assert.assertEquals(FluidAmount.ZERO, FluidAmount.parse("0"));
        Assert.assertEquals(FluidAmount.ONE, FluidAmount.parse("1"));
        Assert.assertEquals(FluidAmount.NEGATIVE_ONE, FluidAmount.parse("-1"));
        Assert.assertEquals(new FluidAmount(416789), FluidAmount.parse("416789"));
        Assert.assertEquals(new FluidAmount(-13416789), FluidAmount.parse("-13416789"));
    }

    @Test
    public void testParseDoubles() {
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("0.1"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("2.8"));
        Assert.assertEquals(FluidAmount.of(3, 10), FluidAmount.parse("0.3"));
        Assert.assertEquals(FluidAmount.of(18927361293L, 100000000000L), FluidAmount.parse("0.18927361293"));
    }

    @Test
    public void testParseFractional() {
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("1/10"));
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("1 / 10"));
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("1 / 10"));
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("0 + 1 / 10"));
        Assert.assertEquals(FluidAmount.of(1, 10), FluidAmount.parse("0 + (1 / 10)"));

        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("28/10"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("28 / 10"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("28 / 10"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("0 + 28 / 10"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("0 + (28 / 10)"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("2 + 8 / 10"));
        Assert.assertEquals(FluidAmount.of(28, 10), FluidAmount.parse("2 + (8 / 10)"));
    }
}
