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

public class SubFixedItemInv extends SubFixedItemInvView implements FixedItemInv {

    public SubFixedItemInv(FixedItemInv inv, int fromIndex, int toIndex) {
        super(inv, fromIndex, toIndex);
    }

    FixedItemInv inv() {
        return (FixedItemInv) this.inv;
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return inv().setInvStack(getInternalSlot(slot), to, simulation);
    }

    @Override
    public FixedItemInv getSubInv(int fIndex, int tIndex) {
        if (fIndex == tIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (fIndex == 0 && tIndex == getSlotCount()) {
            return this;
        }
        fIndex = getInternalSlot(fIndex);
        tIndex = getInternalSlot(tIndex - 1) + 1;
        return create(inv(), fIndex, tIndex);
    }

    @Override
    public FixedItemInv getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        for (int i = 0; i < slots.length; i++) {
            slots[i] = getInternalSlot(slots[i]);
        }
        return MappedFixedItemInv.create(inv(), slots);
    }

    public static SubFixedItemInv create(FixedItemInv inv, int fromIndex, int toIndex) {
        if (inv instanceof ModifiableFixedItemInv) {
            return new OfModifiable((ModifiableFixedItemInv) inv, fromIndex, toIndex);
        }
        if (inv instanceof CopyingFixedItemInv) {
            return new OfCopying((CopyingFixedItemInv) inv, fromIndex, toIndex);
        }

        return new SubFixedItemInv(inv, fromIndex, toIndex);
    }

    public static class OfModifiable extends SubFixedItemInv implements ModifiableFixedItemInv {

        public OfModifiable(FixedItemInv.ModifiableFixedItemInv inv, int fromIndex, int toIndex) {
            super(inv, fromIndex, toIndex);
        }

        @Override
        public void markDirty() {
            ((ModifiableFixedItemInv) inv).markDirty();
        }
    }

    public static class OfCopying extends SubFixedItemInv implements CopyingFixedItemInv {

        public OfCopying(FixedItemInv.CopyingFixedItemInv inv, int fromIndex, int toIndex) {
            super(inv, fromIndex, toIndex);
        }

        @Override
        public ItemStack getUnmodifiableInvStack(int slot) {
            return ((CopyingFixedItemInv) inv).getUnmodifiableInvStack(getInternalSlot(slot));
        }

        @Override
        public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
            FixedItemInvView wrapper = this;
            return ((CopyingFixedItemInv) inv).addListener((realInv, slot, previous, current) -> {
                assert realInv == inv;
                if (slot >= fromIndex && slot < toIndex) {
                    int exposedSlot = slot - fromIndex;
                    listener.onChange(wrapper, exposedSlot, previous, current);
                }
            }, removalToken);
        }
    }
}
