/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import alexiil.mc.lib.attributes.item.FixedItemInvView;

/** Default implementation for {@link FixedItemInvView#getSubInv(int, int)}. */
public class SubFixedItemInvView extends AbstractPartialFixedItemInvView {

    /** The slots that we use. */
    protected final int fromIndex, toIndex;

    public SubFixedItemInvView(FixedItemInvView inv, int fromIndex, int toIndex) {
        super(inv);
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException(
                "fromIndex was greater than toIndex! (" + fromIndex + " > " + toIndex + ")"
            );
        }
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /** @return The slot that the internal {@link #inv} should use. */
    @Override
    protected final int getInternalSlot(int slot) {
        slot += fromIndex;
        if (slot >= toIndex) {
            throw new IllegalArgumentException(
                "The given slot " + (slot - fromIndex) + "is greater than the size of this inventory! ("
                    + getSlotCount() + ")"
            );
        }
        return slot;
    }

    @Override
    public int getSlotCount() {
        return toIndex - fromIndex;
    }

    @Override
    public FixedItemInvView getFixedView() {
        if (getClass() == SubFixedItemInvView.class) {
            return this;
        }
        return super.getFixedView();
    }

    @Override
    public FixedItemInvView getSubInv(int fIndex, int tIndex) {
        if (fIndex == tIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (fIndex == 0 && tIndex == getSlotCount()) {
            return this;
        }
        fIndex = getInternalSlot(fIndex);
        tIndex = getInternalSlot(tIndex - 1) + 1;
        return new SubFixedItemInvView(inv, fIndex, tIndex);
    }

    @Override
    public FixedItemInvView getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        for (int i = 0; i < slots.length; i++) {
            slots[i] = getInternalSlot(slots[i]);
        }
        return new MappedFixedItemInvView(inv, slots);
    }
}
