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
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.AbstractCombined;

public final class CombinedItemExtractable extends AbstractCombined<ItemExtractable> implements ItemExtractable {

    public CombinedItemExtractable(List<? extends ItemExtractable> list) {
        super(list);
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxCount cannot be negative! (was " + maxAmount + ")");
        }
        ItemStack extracted = ItemStack.EMPTY;
        for (ItemExtractable extractable : list) {
            if (extracted.isEmpty()) {
                extracted = extractable.attemptExtraction(filter, maxAmount, simulation);
                if (extracted.isEmpty()) {
                    continue;
                }
                if (extracted.getCount() >= maxAmount) {
                    return extracted;
                }
                filter = new ExactItemStackFilter(extracted);
            } else {
                int newMaxCount = maxAmount - extracted.getCount();
                ItemStack additional = extractable.attemptExtraction(filter, newMaxCount, simulation);
                if (additional.isEmpty()) {
                    continue;
                }
                if (!ItemStackUtil.areEqualIgnoreAmounts(additional, extracted)) {
                    throw new IllegalStateException("bad ItemExtractable " + extractable.getClass().getName());
                }
                extracted.increment(additional.getCount());
                if (extracted.getCount() >= maxAmount) {
                    return extracted;
                }
            }
        }
        return extracted;
    }
}
