/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A simple delegate base class for {@link FixedItemInv}. */
public class DelegatingFixedItemInv implements FixedItemInv {

    protected final FixedItemInv delegate;

    public DelegatingFixedItemInv(FixedItemInv delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlotCount() {
        return delegate.getSlotCount();
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return delegate.getInvStack(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return delegate.isItemValidForSlot(slot, stack);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return delegate.getFilterForSlot(slot);
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        FixedItemInvView wrapper = this;
        return delegate.addListener((realInv, slot, previous, current) -> {
            listener.onChange(wrapper, slot, previous, current);
        }, removalToken);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return delegate.setInvStack(slot, to, simulation);
    }
}
