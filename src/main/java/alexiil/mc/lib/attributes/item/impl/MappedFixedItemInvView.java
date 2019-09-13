/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/** Default implementation for {@link FixedItemInvView#getMappedInv(int...)}. */
public class MappedFixedItemInvView extends AbstractPartialFixedItemInvView {

    protected final int[] slots;
    protected final Int2IntMap inverseSlotMap;

    public MappedFixedItemInvView(FixedItemInvView inv, int[] slots) {
        super(inv);
        this.slots = Arrays.copyOf(slots, slots.length);
        inverseSlotMap = new Int2IntOpenHashMap(slots.length);
        inverseSlotMap.defaultReturnValue(-1);
        for (int i = 0; i < slots.length; i++) {
            int s = slots[i];
            if (s < 0 || s >= inv.getSlotCount()) {
                throw new IllegalArgumentException(
                    "Invalid slot index: " + s + ", as it must be between 0 and the slot count of " + inv.getSlotCount()
                );
            }
            int prev = inverseSlotMap.put(s, i);
            if (prev != -1) {
                throw new IllegalStateException(
                    "Duplicated slot index! (" + s + " appears at both index " + prev + " and " + i + " in " + Arrays
                        .toString(slots) + ")"
                );
            }
        }
    }

    public static MappedFixedItemInvView createView(FixedItemInvView inv, int[] slots) {
        if (inv instanceof FixedItemInv) {
            return MappedFixedItemInv.create((FixedItemInv) inv, slots);
        }
        return new MappedFixedItemInvView(inv, slots);
    }

    @Override
    protected int getInternalSlot(int slot) {
        return slots[slot];
    }

    @Override
    public int getSlotCount() {
        return slots.length;
    }

    @Override
    public FixedItemInvView getSubInv(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }

        int[] nSlots = new int[toIndex - fromIndex];
        int i = 0;
        for (int s = fromIndex; s < toIndex; s++) {
            nSlots[i++] = getInternalSlot(s);
        }
        return new MappedFixedItemInvView(inv, nSlots);
    }

    @Override
    public FixedItemInvView getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        slots = Arrays.copyOf(slots, slots.length);
        for (int i = 0; i < slots.length; i++) {
            slots[i] = getInternalSlot(slots[i]);
        }
        return new MappedFixedItemInvView(inv, slots);
    }
}
