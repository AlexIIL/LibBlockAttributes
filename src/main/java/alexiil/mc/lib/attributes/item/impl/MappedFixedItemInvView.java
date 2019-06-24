/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/** Default implementation for {@link FixedItemInvView#getMappedInv(int...)}. */
public class MappedFixedItemInvView extends AbstractPartialFixedItemInvView {

    private final int[] slots;
    private final Int2IntMap inverseSlotMap;

    public MappedFixedItemInvView(FixedItemInvView inv, int[] slots) {
        super(inv);
        this.slots = Arrays.copyOf(slots, slots.length);
        inverseSlotMap = new Int2IntOpenHashMap(slots.length);
        inverseSlotMap.defaultReturnValue(-1);
        for (int i = 0; i < slots.length; i++) {
            int s = slots[i];
            if (s < 0 || s >= inv.getSlotCount()) {
                throw new IllegalArgumentException("Invalid slot index: " + s
                    + ", as it must be between 0 and the slot count of " + inv.getSlotCount());
            }
            int prev = inverseSlotMap.put(s, i);
            if (prev != -1) {
                throw new IllegalStateException("Duplicated slot index! (" + s + " appears at both index " + prev
                    + " and " + i + " in " + Arrays.toString(slots) + ")");
            }
        }
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
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
        FixedItemInvView wrapper = this;
        return inv.addListener((realInv, slot, previous, current) -> {
            assert realInv == inv;
            int exposedSlot = inverseSlotMap.get(slot);
            if (exposedSlot >= 0) {
                listener.onChange(wrapper, exposedSlot, previous, current);
            }
        }, removalToken);
    }
}
