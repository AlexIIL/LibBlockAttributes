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
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link ItemInsertable} wrapper over an {@link FixedItemInv}. This implementation is the naive implementation
 * where every insertion operation will look at every slot in the target inventory in order to insert into the most
 * appropriate slot first. As such the use of this class is discouraged whenever a more efficient version can be made
 * (unless the target inventory has a very small {@link FixedItemInvView#getSlotCount() size}.
 * 
 * @deprecated Use {@link GroupedItemInvFixedWrapper} instead. */
@Deprecated
public final class SimpleFixedItemInvInsertable implements ItemInsertable {

    private final FixedItemInv inv;

    public SimpleFixedItemInvInsertable(FixedItemInv inv) {
        this.inv = inv;
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
        // ItemStack leftover = stack.copy();
        //
        // // First: scan the available slots to see if we can add to an existing stack
        //
        // IntList slotsModified = new IntArrayList();
        //
        // if (slots == null) {
        // for (int s = 0; s < inv.getInvSize(); s++) {
        // attemptAddToExisting(slotsModified, s, leftover, simulation);
        // }
        // } else {
        // for (int s : slots) {
        // attemptAddToExisting(slotsModified, s, leftover, simulation);
        // }
        // }

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private ItemStack simpleDumbBadInsertionToBeRemoved(ItemStack stack, Simulation simulation) {
        stack = stack.copy();
        for (int s = 0; s < inv.getSlotCount(); s++) {
            ItemStack inSlot = inv.getInvStack(s);
            int current = inSlot.isEmpty() ? 0 : inSlot.getAmount();
            int max = Math.min(current + stack.getAmount(), inv.getMaxAmount(s, stack));
            int addable = max - current;
            if (addable <= 0) {
                continue;
            }
            if (current > 0 && !ItemStackUtil.areEqualIgnoreAmounts(stack, inSlot)) {
                continue;
            }
            if (inSlot.isEmpty()) {
                inSlot = stack.copy();
                inSlot.setAmount(addable);
            } else {
                inSlot.addAmount(addable);
            }
            if (inv.setInvStack(s, inSlot, simulation)) {
                stack.subtractAmount(addable);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }
}
