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
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** An {@link ItemInsertable} wrapper over an {@link FixedItemInv}. This implementation is the naive implementation
 * where every insertion operation will look at every slot in the target inventory in order to insert into the most
 * appropriate slot first. As such the use of this class is discouraged whenever a more efficient version can be made
 * (unless the target inventory has a very small {@link FixedItemInvView#getSlotCount() size}.
 * 
 * @deprecated Use {@link GroupedItemInvFixedWrapper} instead. */
@Deprecated // (since = "0.4.0", forRemoval = true)
public final class SimpleFixedItemInvInsertable implements ItemInsertable {

    private final GroupedItemInvFixedWrapper wrapper;

    public SimpleFixedItemInvInsertable(FixedItemInv inv) {
        this.wrapper = new GroupedItemInvFixedWrapper(inv);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return wrapper.attemptInsertion(stack, simulation);
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return wrapper.getInsertionFilter();
    }
}
