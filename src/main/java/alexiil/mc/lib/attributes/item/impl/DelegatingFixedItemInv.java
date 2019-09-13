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
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A simple delegate base class for {@link FixedItemInv}. */
public class DelegatingFixedItemInv implements FixedItemInv {

    protected final FixedItemInv delegate;

    public DelegatingFixedItemInv(FixedItemInv delegate) {
        this.delegate = delegate;
    }

    public static DelegatingFixedItemInv createDelegate(FixedItemInv inv) {
        if (inv instanceof ModifiableFixedItemInv) {
            return new OfModifiable((ModifiableFixedItemInv) inv);
        }
        if (inv instanceof CopyingFixedItemInv) {
            return new OfCopying((CopyingFixedItemInv) inv);
        }

        return new DelegatingFixedItemInv(inv);
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
    public int getChangeValue() {
        return delegate.getChangeValue();
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return delegate.setInvStack(slot, to, simulation);
    }

    @Override
    public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
        FixedItemInvView wrapper = this;
        return delegate.addListener(inv -> {
            listener.onMarkDirty(wrapper);
        }, removalToken);
    }

    public static class OfModifiable extends DelegatingFixedItemInv implements ModifiableFixedItemInv {

        public OfModifiable(ModifiableFixedItemInv delegate) {
            super(delegate);
        }

        @Override
        public void markDirty() {
            ((ModifiableFixedItemInv) delegate).markDirty();
        }
    }

    public static class OfCopying extends DelegatingFixedItemInv implements CopyingFixedItemInv {

        public OfCopying(CopyingFixedItemInv delegate) {
            super(delegate);
        }

        @Override
        public ItemStack getUnmodifiableInvStack(int slot) {
            return ((CopyingFixedItemInv) delegate).getUnmodifiableInvStack(slot);
        }

        @Override
        public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
            FixedItemInvView wrapper = this;
            return ((CopyingFixedItemInv) delegate).addListener((realInv, slot, previous, current) -> {
                listener.onChange(wrapper, slot, previous, current);
            }, removalToken);
        }
    }
}
