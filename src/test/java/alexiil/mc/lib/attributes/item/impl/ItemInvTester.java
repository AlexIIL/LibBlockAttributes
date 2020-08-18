/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import org.junit.Assert;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.VanillaSetupBaseTester;
import alexiil.mc.lib.attributes.item.FixedItemInv;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;

public abstract class ItemInvTester extends VanillaSetupBaseTester {

    public static final Item[] ITEMS;

    static {
        ITEMS = new Item[50];

        int i = 0;
        for (Item item : Registry.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ITEMS[i++] = item;
            if (i == 50) break;
        }
    }

    public static FixedItemInv[] createInventories(Int2ObjectFunction<FixedItemInv> ctor) {
        FixedItemInv inv1 = ctor.apply(10);
        fillInventory(inv1, ITEMS, 0);

        FixedItemInv inv2 = ctor.apply(10);
        fillInventory(inv2, ITEMS, 10);

        FixedItemInv inv3 = ctor.apply(10);
        fillInventory(inv3, ITEMS, 20);
        return new FixedItemInv[] { inv1, inv2, inv3 };
    }

    public static void fillInventory(FixedItemInv dest, Item[] src, int srcIndex) {
        for (int i = 0; i < dest.getSlotCount(); i++) {
            dest.forceSetInvStack(i, new ItemStack(src[srcIndex + i]));
        }
    }

    public static void assertIdentityEquals(Object expected, Object actual) {
        if (expected == actual) {
            return;
        }
        Assert.fail("Expected \n" + expected + "\n and \n" + actual + "\n to be '==', but they weren't!");
    }

    public static void assertItem(Item item, ItemStack stack) {
        Assert.assertFalse(stack.isEmpty());
        Assert.assertEquals(item, stack.getItem());
    }

    public static void assertEquals(ItemStack expected, ItemStack actual) {
        if (!ItemStack.areEqual(expected, actual)) {
            Assert.fail("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    public static void assertEmpty(ItemStack actual) {
        assertEquals(ItemStack.EMPTY, actual);
    }
}
