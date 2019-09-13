/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A delegating view of a single slot in a {@link FixedItemInvView}. */
public class SingleItemSlotView {

    final FixedItemInvView backingView;
    final int slot;

    SingleItemSlotView(FixedItemInvView backingView, int slot) {
        this.backingView = backingView;
        this.slot = slot;
    }

    public FixedItemInvView getBackingInv() {
        return backingView;
    }

    public final int getIndex() {
        return slot;
    }

    public final ItemStack get() {
        return backingView.getInvStack(slot);
    }

    public final int getMaxAmount(ItemStack stack) {
        return backingView.getMaxAmount(slot, stack);
    }

    public final boolean isValid(ItemStack stack) {
        return backingView.isItemValidForSlot(slot, stack);
    }

    public final ItemFilter getFilter() {
        return backingView.getFilterForSlot(slot);
    }
}
