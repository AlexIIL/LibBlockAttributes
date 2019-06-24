/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** Base class for {@link SubFixedItemInvView} and {@link MappedFixedItemInvView}. */
public abstract class AbstractPartialFixedItemInvView implements FixedItemInvView {

    /** The inventory that is wrapped. */
    protected final FixedItemInvView inv;

    protected AbstractPartialFixedItemInvView(FixedItemInvView inv) {
        this.inv = inv;
    }

    /** @return The slot that the internal {@link #inv} should use. */
    protected abstract int getInternalSlot(int slot);

    @Override
    public ItemStack getInvStack(int slot) {
        return inv.getInvStack(getInternalSlot(slot));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return inv.isItemValidForSlot(getInternalSlot(slot), stack);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return inv.getFilterForSlot(getInternalSlot(slot));
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return inv.getMaxAmount(getInternalSlot(slot), stack);
    }
}
