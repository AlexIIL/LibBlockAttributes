/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import org.junit.Test;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.LimitedGroupedItemInv;

public class SimpleLimitedGroupedItemInvTester extends ItemInvTester {

    @Test
    public void testBasicLimits() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        fillInventory(inv, ITEMS, 0);
        GroupedItemInv grouped = inv.getGroupedInv();
        LimitedGroupedItemInv limited = grouped.createLimitedGroupedInv();

        limited.getRule(new ItemStack(ITEMS[0])).disallowTransfer();

        assertEquals(ItemStack.EMPTY, limited.extract(new ItemStack(ITEMS[0]), 1));
        assertEquals(new ItemStack(ITEMS[0]), grouped.extract(new ItemStack(ITEMS[0]), 1));

        limited.getRule(stack -> stack.getItem() != ITEMS[2]).disallowTransfer();

        assertEquals(ItemStack.EMPTY, limited.extract(new ItemStack(ITEMS[1]), 1));
        assertEquals(new ItemStack(ITEMS[1]), grouped.extract(new ItemStack(ITEMS[1]), 1));

        assertEquals(new ItemStack(ITEMS[2]), limited.extract(new ItemStack(ITEMS[2]), 1));
        assertEquals(ItemStack.EMPTY, grouped.extract(new ItemStack(ITEMS[2]), 1));
    }
}
