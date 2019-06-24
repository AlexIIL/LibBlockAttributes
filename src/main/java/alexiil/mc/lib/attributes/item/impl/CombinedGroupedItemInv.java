/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class CombinedGroupedItemInv extends CombinedGroupedItemInvView implements GroupedItemInv {

    public CombinedGroupedItemInv(List<? extends GroupedItemInv> inventories) {
        super(inventories);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        for (GroupedItemInvView view : this.inventories) {
            GroupedItemInv inv = (GroupedItemInv) view;
            stack = inv.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return stack;
            }
        }
        return stack;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        ItemFilter filter = ConstantItemFilter.NOTHING;
        for (GroupedItemInvView view : this.inventories) {
            GroupedItemInv inv = (GroupedItemInv) view;
            filter = filter.or(inv.getInsertionFilter());
        }
        return filter;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        ItemStack extracted = ItemStack.EMPTY;
        for (GroupedItemInvView view : this.inventories) {
            ItemExtractable extractable = (ItemExtractable) view;
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
                filter = new ExactItemStackFilter(extracted);
            } else {
                int newMaxCount = maxAmount - extracted.getAmount();
                ItemStack additional = extractable.attemptExtraction(filter, newMaxCount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                if (!ItemStackUtil.areEqualIgnoreAmounts(additional, extracted)) {
                    throw new IllegalStateException("bad ItemExtractable " + extractable.getClass().getName());
                }
                extracted.addAmount(additional.getAmount());
                if (extracted.getAmount() >= maxAmount) {
                    return extracted;
                }
            }
        }
        return extracted;
    }
}
