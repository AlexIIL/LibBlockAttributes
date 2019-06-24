/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A {@link GroupedItemInv} wrapper over a {@link FixedItemInv}. This implementation is the naive implementation where
 * every insertion operation will look at every slot in the target inventory in order to insert into the most
 * appropriate slot first. As such the use of this class is discouraged whenever a more efficient version can be used
 * (unless the target inventory has a very small {@link FixedItemInvView#getSlotCount() size}). */
public class GroupedItemInvFixedWrapper extends GroupedItemInvViewFixedWrapper implements GroupedItemInv {

    public GroupedItemInvFixedWrapper(FixedItemInv inv) {
        super(inv);
    }

    final FixedItemInv inv() {
        return (FixedItemInv) inv;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        int invSize = inv.getSlotCount();
        switch (invSize) {
            case 0: {
                // What?
                return ConstantItemFilter.NOTHING;
            }
            case 1: {
                return inv.getFilterForSlot(0);
            }
            case 2: {
                return inv.getFilterForSlot(0).and(inv.getFilterForSlot(1));
            }
            default: {
                List<ItemFilter> filters = new ArrayList<>(invSize);
                for (int i = 0; i < invSize; i++) {
                    filters.add(inv.getFilterForSlot(i));
                }
                return AggregateItemFilter.anyOf(filters);
            }
        }
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        /* Even though there is a giant warning at the top of this class it should still be possible to optimise this
         * implementation a bit more than this very basic version. */

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private ItemStack simpleDumbBadInsertionToBeRemoved(ItemStack stack, Simulation simulation) {
        stack = stack.copy();
        for (int s = 0; s < inv.getSlotCount(); s++) {
            stack = ItemInvUtil.insertSingle(inv(), s, stack, simulation);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxCount, Simulation simulation) {
        if (maxCount < 0) {
            throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxCount + ")");
        }
        ItemStack stack = ItemStack.EMPTY;
        if (maxCount == 0) {
            return stack;
        }
        for (int s = 0; s < inv.getSlotCount(); s++) {
            stack = ItemInvUtil.extractSingle(inv(), s, filter, stack, maxCount - stack.getAmount(), simulation);
            if (stack.getAmount() >= maxCount) {
                return stack;
            }
        }
        return stack;
    }
}
