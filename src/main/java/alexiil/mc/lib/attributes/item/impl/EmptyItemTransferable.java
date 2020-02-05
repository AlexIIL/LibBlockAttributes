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
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemTransferable;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** An {@link ItemTransferable} that never returns any items from
 * {@link #attemptExtraction(ItemFilter, int, Simulation)}, nor accepts any items in
 * {@link #attemptInsertion(ItemStack, Simulation)}. */
public enum EmptyItemTransferable implements ItemTransferable, NullVariant {
    /** An {@link ItemTransferable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being transferable should be considered FALSE for this instance. */
    NULL,

    /** An {@link ItemTransferable} that informs callers that it will interact with a nearby {@link ItemTransferable},
     * {@link ItemExtractable}, or {@link ItemInsertable} but doesn't expose any other item based attributes. */
    CONTROLLER;

    private final String str = "EmptyItemTransferable." + name();

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return ConstantItemFilter.NOTHING;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemInsertable getPureInsertable() {
        return this == NULL ? RejectingItemInsertable.NULL : RejectingItemInsertable.EXTRACTOR;
    }

    @Override
    public ItemExtractable getPureExtractable() {
        return this == NULL ? EmptyItemExtractable.NULL : EmptyItemExtractable.SUPPLIER;
    }

    @Override
    public String toString() {
        return str;
    }
}
