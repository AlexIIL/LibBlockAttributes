/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link ItemInsertable} that delegates to another {@link ItemInsertable}, but has an additional filter as to what
 * can be inserted. */
public final class FilteredItemInsertable implements ItemInsertable {

    private final ItemInsertable real;
    public final ItemFilter filter;

    public FilteredItemInsertable(ItemInsertable real, ItemFilter filter) {
        this.real = real;
        this.filter = filter;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return real.getInsertionFilter().and(filter);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        if (stack.isEmpty() || !filter.matches(stack)) {
            return stack;
        }
        return real.attemptInsertion(stack, simulation);
    }

    @Override
    public ItemInsertable filtered(ItemFilter filter) {
        return new FilteredItemInsertable(real, filter.and(this.filter));
    }
}
