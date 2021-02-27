/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.AbstractCombined;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/** An {@link FixedItemInvView} that delegates to a list of them instead of storing items directly. */
public class CombinedFixedItemInvView<InvType extends FixedItemInvView> extends AbstractCombined<InvType> implements FixedItemInvView {

    public final List<? extends InvType> views;
    protected final int[] subSlotStartIndex;
    protected final int invSize;

    public CombinedFixedItemInvView(List<? extends InvType> views) {
        super(views);
        this.views = views;
        subSlotStartIndex = new int[views.size()];
        int size = 0;
        for (int i = 0; i < views.size(); i++) {
            subSlotStartIndex[i] = size;
            FixedItemInvView view = views.get(i);
            int s = view.getSlotCount();
            size += s;
        }
        invSize = size;
    }

    public static FixedItemInvView createView(List<? extends FixedItemInvView> list) {
        if (list.isEmpty()) {
            return EmptyFixedItemInv.INSTANCE;
        } else if (list.size() == 1) {
            return list.get(0);
        }

        List<FixedItemInv> normal = new ArrayList<>();
        for (FixedItemInvView view : list) {
            if (view instanceof FixedItemInv) {
                normal.add((FixedItemInv) view);
                continue;
            }
            return new CombinedFixedItemInvView<>(list);
        }

        return CombinedFixedItemInv.create(normal);
    }

    @Override
    public int getSlotCount() {
        return invSize;
    }

    protected int getInvIndex(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative! (was " + slot + ")");
        }

        for (int i = 1; i < subSlotStartIndex.length; i++) {
            int startIndex = subSlotStartIndex[i];
            if (slot < startIndex) {
                return i - 1;
            }
        }

        if (slot < invSize) {
            return views.size() - 1;
        }

        throw new IllegalArgumentException(
            "Slot must be less than getInvSize() (was " + slot + ", maximum slot is " + (invSize - 1) + ")"
        );
    }

    protected InvType getInv(int slot) {
        return views.get(getInvIndex(slot));
    }

    protected int getSubSlot(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Slot must be non-negative! (was " + slot + ")");
        }

        for (int i = 0; i < subSlotStartIndex.length; i++) {
            int startIndex = subSlotStartIndex[i];
            if (slot < startIndex) {
                if (i == 0) {
                    return slot;
                }
                return slot - subSlotStartIndex[i - 1];
            }
        }
        if (slot < invSize) {
            return slot - subSlotStartIndex[subSlotStartIndex.length - 1];
        }

        throw new IllegalArgumentException(
            "Slot must be less than getInvSize() (was " + slot + ", maximum slot is " + (invSize - 1) + ")"
        );
    }

    @Override
    public ItemStack getInvStack(int slot) {
        return getInv(slot).getInvStack(getSubSlot(slot));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return getInv(slot).isItemValidForSlot(getSubSlot(slot), item);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return getInv(slot).getFilterForSlot(getSubSlot(slot));
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        return getInv(slot).getMaxAmount(getSubSlot(slot), stack);
    }

    @Override
    public int getChangeValue() {
        int count = 0;
        for (InvType inv : views) {
            count += inv.getChangeValue();
        }
        return count;
    }

    @Override
    public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[views.size()];
        final ListenerRemovalToken ourRemToken = new ListenerRemovalToken() {

            boolean hasAlreadyRemoved = false;

            @Override
            public void onListenerRemoved() {
                for (ListenerToken token : tokens) {
                    if (token == null) {
                        // This means we have only half-initialised
                        // (and all of the next tokens must also be null)
                        return;
                    }
                    token.removeListener();
                }
                if (!hasAlreadyRemoved) {
                    hasAlreadyRemoved = true;
                    removalToken.onListenerRemoved();
                }
            }
        };

        final FixedItemInvView wrapper = this;
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = views.get(i).addListener((inv) -> {
                listener.onMarkDirty(wrapper);
            }, ourRemToken);
            if (tokens[i] == null) {
                for (int j = 0; j < i; j++) {
                    tokens[j].removeListener();
                }
                return null;
            }
        }
        return () -> {
            for (ListenerToken token : tokens) {
                token.removeListener();
            }
        };
    }

    @Override
    public FixedItemInvView getSubInv(int fromIndex, int toIndex) {

        if (fromIndex == toIndex) {
            return EmptyFixedItemInv.INSTANCE;
        }

        if (fromIndex == 0 && toIndex == getSlotCount()) {
            return this;
        }

        int invFrom = getInvIndex(fromIndex);
        int invTo = getInvIndex(toIndex - 1);

        int slotFrom = getSubSlot(fromIndex);
        int slotTo = getSubSlot(toIndex - 1) + 1;

        if (invFrom == invTo) {
            return views.get(invFrom).getSubInv(slotFrom, slotTo);
        }
        List<? extends InvType> subList = views.subList(invFrom, invTo + 1);

        if (slotFrom == 0 && slotTo == subList.get(subList.size() - 1).getSlotCount()) {
            return createView(subList);
        }

        List<FixedItemInvView> list = new ArrayList<>(subList);
        list.set(0, list.get(0).getSubInv(slotFrom, list.get(0).getSlotCount()));
        list.set(list.size() - 1, list.get(list.size() - 1).getSubInv(0, slotTo));
        return createView(list);
    }

    @Override
    public FixedItemInvView getMappedInv(int... slots) {
        if (slots.length == 0) {
            return EmptyFixedItemInv.INSTANCE;
        }
        if (FixedItemInvView.areSlotArraysEqual(this, slots)) {
            return this;
        }

        // Create a mapped inventory that's composed of mapped sub-inventories
        // (Rather than just this inventory)

        // To do this we need to unpick the slots back into the order of the underlying inventories
        // and then put them back together for the main one.

        Int2ObjectMap<IntList> invSlotCount = new Int2ObjectRBTreeMap<>();
        int[] inventoryIndexes = new int[slots.length];
        int[] inventorySlots = new int[slots.length];
        IntSet seenSlots = new IntOpenHashSet();

        for (int i = 0; i < slots.length; i++) {

            if (!seenSlots.add(slots[i])) {
                throw new IllegalArgumentException("Duplicate slot " + slots[i] + " " + Arrays.toString(slots));
            }

            int inv = getInvIndex(slots[i]);
            IntList list = invSlotCount.get(inv);
            if (list == null) {
                list = new IntArrayList();
                invSlotCount.put(inv, list);
            }
            int sub = getSubSlot(slots[i]);
            list.add(sub);
            inventoryIndexes[i] = inv;
            inventorySlots[i] = sub;
        }
        if (invSlotCount.size() == 1) {
            // We're only mapping to a single inventory
            return views.get(invSlotCount.keySet().iterator().nextInt()).getMappedInv(inventorySlots);
        }

        List<FixedItemInvView> invs = new ArrayList<>();
        for (Int2ObjectMap.Entry<IntList> entry : invSlotCount.int2ObjectEntrySet()) {
            int inv = entry.getIntKey();
            IntList invSlots = entry.getValue();
            invs.add(views.get(inv).getMappedInv(invSlots.toIntArray()));
        }

        int[] mappedSlots = new int[slots.length];
        int[] inventorySlotCounter = new int[invs.size()];
        int[] inventoryStartIndexes = new int[invs.size()];

        int currentIndex = 0;
        for (int i = 0; i < invs.size(); i++) {
            inventoryIndexes[i] = currentIndex;
            currentIndex += invs.get(i).getSlotCount();
        }

        for (int i = 0; i < slots.length; i++) {
            int inv = inventoryIndexes[i];
            int invCount = inventorySlotCounter[inv]++;
            mappedSlots[i] = invCount + inventoryStartIndexes[inv];
        }

        FixedItemInvView combined = createView(invs);
        return MappedFixedItemInvView.createView(combined, mappedSlots);
    }
}
