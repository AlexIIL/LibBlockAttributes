/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.LimitedFixedItemInv;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleLimitedFixedItemInvTester extends ItemInvTester {

    private static ItemStack apples(int count) {
        return new ItemStack(Items.APPLE, count);
    }

    @Test
    public void testInsertionWithFilter() {
        FullFixedItemInv inv = new FullFixedItemInv(10);

        AtomicBoolean allowInsert = new AtomicBoolean();
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0)
                .filterInserts(stack -> allowInsert.get());

        // This insertion should fail
        ItemStack excess = limitedInv.insertStack(0, apples(1), Simulation.ACTION);
        assertEquals(apples(1), excess);
        assertEmpty(inv.getInvStack(0));

        // Now it should succeed
        allowInsert.set(true);
        excess = limitedInv.insertStack(0, apples(1), Simulation.ACTION);
        assertEmpty(excess);
        assertEquals(apples(1), inv.getInvStack(0));
    }

    @Test
    public void testDontCallFilterForEmptyStacks() {
        FullFixedItemInv inv = new FullFixedItemInv(10);

        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0)
                .filterInserts(stack -> {
                    Assert.fail("Should not be called.");
                    return false;
                });

        assertEmpty(limitedInv.insertStack(0, ItemStack.EMPTY, Simulation.ACTION));
    }

    @Test
    public void testInsertionFailsAtMaxAmount() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(10));

        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0).limitInsertionCount(10);

        ItemStack excess = limitedInv.insertStack(0, apples(1), Simulation.ACTION);
        assertEquals(apples(1), excess);
    }

    @Test
    public void testInsertionBeyondMaxAmountReturnsExcess() {
        // Set it to 5 apples
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(5));

        // Limit to 10 apples
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0).limitInsertionCount(10);

        // Insert 6, leaves excess of 1
        ItemStack excess = limitedInv.insertStack(0, apples(6), Simulation.ACTION);
        assertEquals(apples(1), excess);
    }

    @Test
    public void testInsertionBeyondMaxAmountReturnsExcessForPartialSuccess() {
        // Set it to 5 apples
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(5));

        // Insert a hidden limit to 6 apples
        LimitedFixedItemInv innerLimit = inv.createLimitedFixedInv();
        innerLimit.getAllRule().limitInsertionCount(6);

        // Wrap another limited inventory around with a limit of 10 apples
        LimitedFixedItemInv limitedInv = innerLimit.createLimitedFixedInv();
        limitedInv.getRule(0).limitInsertionCount(10);

        // Insert 6, leaves excess of _5_ because the inner limit should be respected
        ItemStack excess = limitedInv.insertStack(0, apples(6), Simulation.ACTION);
        assertEquals(apples(5), excess);
    }

    @Test
    public void testInsertionUpToMaxAmount() {
        // Set it to 5 apples
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(5));

        // Limit to 10 apples
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0).limitInsertionCount(10);

        // Insert 5, leaves with no excess
        ItemStack excess = limitedInv.insertStack(0, apples(5), Simulation.ACTION);
        assertEmpty(excess);
    }

    @Test
    public void testExtractionWithFilter() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(1));

        AtomicBoolean allowExtract = new AtomicBoolean();
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getRule(0)
                .filterExtracts(stack -> allowExtract.get());

        // This extraction should fail
        ItemStack result = limitedInv.extractStack(0, null, ItemStack.EMPTY, 1, Simulation.ACTION);
        assertEmpty(result);
        assertEquals(apples(1), inv.getInvStack(0));

        // Now it should succeed
        allowExtract.set(true);
        result = limitedInv.extractStack(0, null, ItemStack.EMPTY, 1, Simulation.ACTION);
        assertEquals(apples(1), result);
        assertEmpty(inv.getInvStack(0));
    }

    @Test
    public void testExtractionFailsWhenAtMinimumAmount() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(2));

        // Set the minimum to two items per slot
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getAllRule().setMinimum(2);

        // Try extracting 1 apple
        assertEmpty(limitedInv.extractStack(0, null, ItemStack.EMPTY, 1, Simulation.ACTION));
        assertEquals(apples(2), inv.getInvStack(0));
    }

    @Test
    public void testExtractionUpToMinimumAmount() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        inv.forceSetInvStack(0, apples(2));

        // Set the minimum to one item per slot
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getAllRule().setMinimum(1);

        // Try extracting 2 apples
        ItemStack extracted = limitedInv.extractStack(0, null, ItemStack.EMPTY, 2, Simulation.ACTION);

        // It should only extract 1
        assertEquals(apples(1), extracted);
        assertEquals(apples(1), inv.getInvStack(0));
    }

    @Test
    public void testDontCallFilterWithEmptyStacks() {
        FullFixedItemInv inv = new FullFixedItemInv(10);
        LimitedFixedItemInv limitedInv = inv.createLimitedFixedInv();
        limitedInv.getAllRule().filterExtracts(stack -> {
            Assert.fail("This should not be called");
            return false;
        });

        limitedInv.extractStack(0, null, ItemStack.EMPTY, 1, Simulation.ACTION);
    }
}
