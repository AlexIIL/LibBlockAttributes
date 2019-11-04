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
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public final class CombinedItemInsertable implements ItemInsertable {

    private final List<? extends ItemInsertable> insertables;

    public CombinedItemInsertable(List<? extends ItemInsertable> list) {
        this.insertables = list;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        for (ItemInsertable insertable : insertables) {
            stack = insertable.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        List<ItemFilter> filters = new ArrayList<>(insertables.size());
        for (int i = 0; i < insertables.size(); i++) {
            filters.add(insertables.get(i).getInsertionFilter());
        }
        return AggregateItemFilter.anyOf(filters);
    }
}
