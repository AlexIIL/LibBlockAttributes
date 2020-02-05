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
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** An {@link ItemInsertable} that always refuses to accept any inserted {@link ItemStack}. */
public enum RejectingItemInsertable implements ItemInsertable, NullVariant {
    NULL,
    EXTRACTOR;

    private final String str = "RejectingItemInsertable." + name();

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return ConstantItemFilter.NOTHING;
    }

    @Override
    public ItemInsertable getPureInsertable() {
        return this;
    }

    @Override
    public String toString() {
        return str;
    }
}
