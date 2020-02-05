/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.minecraft.inventory.BasicInventory;

import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.compat.FixedInventoryVanillaWrapper;

public class FixedItemInvTester extends ItemInvTester {

    @Test
    public void testBasics() {

        // Just to make sure the very basics work

        FixedItemInv[] invs = createInventories();
        FixedItemInv inv1 = invs[0];
        FixedItemInv inv2 = invs[1];
        FixedItemInv inv3 = invs[2];

        for (int i = 0; i < 10; i++) {
            assertItem(ITEMS[i], inv1.getInvStack(i));
            assertItem(ITEMS[i + 10], inv2.getInvStack(i));
            assertItem(ITEMS[i + 20], inv3.getInvStack(i));
        }
    }

    @Test
    public void testSubInventories() {

        FixedItemInv[] invs = createInventories();
        FixedItemInv inv1 = invs[0];
        FixedItemInv inv2 = invs[1];
        FixedItemInv inv3 = invs[2];

        assertIdentityEquals(inv1, inv1.getSubInv(0, 10));
        assertIdentityEquals(EmptyFixedItemInv.INSTANCE, inv1.getSubInv(4, 4));

        FixedItemInv sub1 = inv1.getSubInv(3, 5);
        Assert.assertEquals(2, sub1.getSlotCount());
        assertItem(ITEMS[3], sub1.getInvStack(0));
        assertItem(ITEMS[4], sub1.getInvStack(1));
        assertIdentityEquals(EmptyFixedItemInv.INSTANCE, sub1.getSubInv(1, 1));

        FixedItemInv sub2 = sub1.getSubInv(1, 2);
        Assert.assertEquals(1, sub2.getSlotCount());
        assertItem(ITEMS[4], sub2.getInvStack(0));
        assertIdentityEquals(EmptyFixedItemInv.INSTANCE, sub1.getSubInv(0, 0));
    }

    @Test
    public void testMappedInventories() {

        FixedItemInv[] invs = createInventories();
        FixedItemInv inv1 = invs[0];
        FixedItemInv inv2 = invs[1];
        FixedItemInv inv3 = invs[2];

        FixedItemInv map1 = inv1.getMappedInv(4, 7, 3, 8, 2, 9, 0, 5, 6, 1);
        Assert.assertEquals(10, map1.getSlotCount());
        assertItem(ITEMS[4], map1.getInvStack(0));
        assertItem(ITEMS[7], map1.getInvStack(1));
        assertItem(ITEMS[3], map1.getInvStack(2));
        assertItem(ITEMS[8], map1.getInvStack(3));
        assertItem(ITEMS[2], map1.getInvStack(4));
        assertItem(ITEMS[9], map1.getInvStack(5));
        assertItem(ITEMS[0], map1.getInvStack(6));
        assertItem(ITEMS[5], map1.getInvStack(7));
        assertItem(ITEMS[6], map1.getInvStack(8));
        assertItem(ITEMS[1], map1.getInvStack(9));

        assertIdentityEquals(inv1, map1.getMappedInv(6, 9, 4, 2, 0, 7, 8, 1, 3, 5));

        FixedItemInv map2 = inv2.getMappedInv(5, 1, 7, 8);
        Assert.assertEquals(4, map2.getSlotCount());
        assertItem(ITEMS[15], map2.getInvStack(0));
        assertItem(ITEMS[11], map2.getInvStack(1));
        assertItem(ITEMS[17], map2.getInvStack(2));
        assertItem(ITEMS[18], map2.getInvStack(3));

        assertIdentityEquals(map2, map2.getMappedInv(0, 1, 2, 3));
        assertIdentityEquals(map2, map2.getSubInv(0, 4));
    }

    @Test
    public void testCombinedInventories() {

        FixedItemInv[] invs = createInventories();
        FixedItemInv inv1 = invs[0];
        FixedItemInv inv2 = invs[1];
        FixedItemInv inv3 = invs[2];

        FixedItemInv combined123 = CombinedFixedItemInv.create(Arrays.asList(invs));
        assertIdentityEquals(inv1, combined123.getSubInv(0, 10));
        assertIdentityEquals(inv2, combined123.getSubInv(10, 20));
        assertIdentityEquals(inv3, combined123.getSubInv(20, 30));

        for (int i = 0; i < 30; i++) {
            assertItem(ITEMS[i], combined123.getInvStack(i));
        }

        assertIdentityEquals(inv2, combined123.getSubInv(10, 30).getSubInv(0, 10));
        assertIdentityEquals(inv2, combined123.getSubInv(3, 27).getSubInv(7, 17));
    }

    public static FixedItemInv[] createInventories() {
        return createInventories(i -> new FixedInventoryVanillaWrapper(new BasicInventory(i)));
    }
}
