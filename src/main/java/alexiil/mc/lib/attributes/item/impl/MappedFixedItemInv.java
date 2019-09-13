/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;

public class MappedFixedItemInv extends MappedFixedItemInvView implements FixedItemInv {

    public MappedFixedItemInv(FixedItemInv inv, int[] slots) {
        super(inv, slots);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        return ((FixedItemInv) inv).setInvStack(getInternalSlot(slot), to, simulation);
    }

    @Override
    public FixedItemInv getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (fromIndex == 0 && toIndex == getSlotCount()) {
            return this;
        }
        int[] nSlots = new int[toIndex - fromIndex];
        int i = 0;
        for (int s = fromIndex; s < toIndex; s++) {
            nSlots[i++] = getInternalSlot(s);
        }
        return create((FixedItemInv) inv, nSlots);
    }

    @Override
    public FixedItemInv getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (FixedItemInvView.areSlotArraysEqual(this, slots)) {
            return this;
        }
        slots = Arrays.copyOf(slots, slots.length);
        for (int i = 0; i < slots.length; i++) {
            slots[i] = getInternalSlot(slots[i]);
        }
        if (FixedItemInvView.areSlotArraysEqual(inv, slots)) {
            return (FixedItemInv) inv;
        }
        return create((FixedItemInv) inv, slots);
    }

    public static MappedFixedItemInv create(FixedItemInv inv, int[] slots) {
        if (inv instanceof ModifiableFixedItemInv) {
            return new OfModifiable((ModifiableFixedItemInv) inv, slots);
        }
        if (inv instanceof CopyingFixedItemInv) {
            return new OfCopying((CopyingFixedItemInv) inv, slots);
        }

        return new MappedFixedItemInv(inv, slots);
    }

    public static class OfModifiable extends MappedFixedItemInv implements ModifiableFixedItemInv {

        public OfModifiable(FixedItemInv.ModifiableFixedItemInv inv, int[] slots) {
            super(inv, slots);
        }

        @Override
        public void markDirty() {
            ((ModifiableFixedItemInv) inv).markDirty();
        }
    }

    public static class OfCopying extends MappedFixedItemInv implements CopyingFixedItemInv {

        public OfCopying(FixedItemInv.CopyingFixedItemInv inv, int[] slots) {
            super(inv, slots);
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
                int exposedSlot = inverseSlotMap.get(slot);
                if (exposedSlot >= 0) {
                    listener.onChange(wrapper, exposedSlot, previous, current);
                }
            }, removalToken);
        }
    }
}
