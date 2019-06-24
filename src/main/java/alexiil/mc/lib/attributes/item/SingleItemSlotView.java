/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
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

    /** Adds the given listener to the backing inventory, such that the
     * {@link ItemInvSlotChangeListener#onChange(FixedItemInvView, int, ItemStack, ItemStack)} will be called every time
     * that this inventory changes. However if this inventory doesn't support listeners then this will return a null
     * {@link ListenerToken token}.
     * 
     * @param removalToken A token that will be called whenever the given listener is removed from this inventory (or if
     *            this inventory itself is unloaded or otherwise invalidated).
     * @return A token that represents the listener, or null if the listener could not be added. */
    public final ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        return backingView.addListener((realInv, s, previous, current) -> {
            assert realInv == backingView;
            if (slot == s) {
                listener.onChange(realInv, slot, previous, current);
            }
        }, removalToken);
    }
}
