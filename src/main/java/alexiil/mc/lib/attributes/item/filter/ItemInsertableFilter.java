/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.filter;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.ItemInsertable;

/** An {@link ItemFilter} that checks to see if the given {@link ItemInsertable} could have the fluid inserted into it,
 * right now. (Note that this doesn't match the definition of {@link ItemInsertable#getInsertionFilter()}, so you should
 * never use it a return value from that). */
public final class ItemInsertableFilter implements ItemFilter {

    public final ItemInsertable insertable;

    public ItemInsertableFilter(ItemInsertable insertable) {
        this.insertable = insertable;
    }

    @Override
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            // Bit strange, because inserting an empty stack will always return
            // the empty stack, which indicates a successful insertion.
            return true;
        }
        ItemStack leftover = insertable.attemptInsertion(stack, Simulation.SIMULATE);
        return leftover.isEmpty() || leftover.getCount() < stack.getCount();
    }
}
