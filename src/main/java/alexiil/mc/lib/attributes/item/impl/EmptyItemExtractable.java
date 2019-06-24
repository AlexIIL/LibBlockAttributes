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
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link ItemExtractable} that never returns any items from
 * {@link #attemptExtraction(ItemFilter, int, Simulation)}. */
public enum EmptyItemExtractable implements ItemExtractable {
    /** An {@link ItemExtractable} that should be treated as equal to null in all circumstances - that is any checks
     * that depend on an object being extractable should be considered FALSE for this instance. */
    NULL,

    /** An {@link ItemExtractable} that informs callers that it will push items into a nearby {@link ItemInsertable},
     * but doesn't expose any other item based attributes.
     * <p>
     * The buildcraft quarry is a good example of this - it doesn't have any inventory slots itself and it pushes items
     * out of it as it mines them from the world, but item pipes should still connect to it so that it can insert into
     * them. */
    SUPPLIER;

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxCount, Simulation simulation) {
        return ItemStack.EMPTY;
    }
}
