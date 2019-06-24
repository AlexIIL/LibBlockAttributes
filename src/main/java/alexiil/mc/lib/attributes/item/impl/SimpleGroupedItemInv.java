/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple {@link GroupedItemInv} that has a limit on both the number of different items that this can store, and the
 * total number of items that can be stored. */
public class SimpleGroupedItemInv implements GroupedItemInv {

    private static final ItemInvAmountChangeListener[] NO_LISTENERS = new ItemInvAmountChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final ItemInvAmountChangeListener[] INVALIDATING_LISTENERS = new ItemInvAmountChangeListener[0];

    public final int maxItemTypes;
    public final int maxItems;

    /** A cached count of the number of items stored in {@link #stacks}. */
    private int cachedItemCount;

    private ItemInvAmountChangeListener ownerListener;

    private final Map<ItemInvAmountChangeListener, ListenerRemovalToken> listeners =
        new Object2ObjectLinkedOpenCustomHashMap<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private ItemInvAmountChangeListener[] bakedListeners = NO_LISTENERS;

    private final Object2IntMap<ItemStack> stacks = ItemStackCollections.intMap();

    public SimpleGroupedItemInv(int maxItemTypes, int maxItems) {
        this.maxItemTypes = maxItemTypes;
        this.maxItems = maxItems;
        stacks.defaultReturnValue(-1);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return stacks.keySet();
    }

    @Override
    public int getTotalCapacity() {
        return maxItems;
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        int totalCount = 0;
        int totalSpace = maxItems;
        for (Object2IntMap.Entry<ItemStack> entry : stacks.object2IntEntrySet()) {
            ItemStack stack = entry.getKey();
            int count = entry.getIntValue();
            if (filter.matches(stack)) {
                totalCount += count;
            } else {
                totalSpace -= count;
            }
        }
        return new ItemInvStatistic(filter, totalCount, 0, totalSpace);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        int current = stacks.getInt(stack);
        if (current < 0 && stacks.size() >= maxItemTypes) {
            return stack;
        }
        int insertable = Math.min(stack.getAmount(), maxItems - cachedItemCount);
        if (insertable <= 0) {
            return stack;
        }
        stack = stack.copy();
        ItemStack insStack = stack.split(insertable);
        assert insStack.getAmount() == insertable;
        if (simulation == Simulation.ACTION) {
            stacks.put(insStack, insertable + (current < 0 ? 0 : current));
            cachedItemCount += insertable;
            fireAmountChange(stack, current, current + insertable);
        }
        return stack;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        for (Object2IntMap.Entry<ItemStack> entry : stacks.object2IntEntrySet()) {
            ItemStack stack = entry.getKey();
            if (filter.matches(stack)) {
                int current = entry.getIntValue();
                int extracted = Math.min(current, maxAmount);
                if (simulation == Simulation.ACTION) {
                    if (extracted == current) {
                        stacks.removeInt(stack);
                    } else {
                        entry.setValue(current - extracted);
                    }
                    cachedItemCount -= extracted;
                    fireAmountChange(stack, current, current - extracted);
                }
                stack = stack.copy();
                stack.setAmount(extracted);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // Listeners

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        if (bakedListeners == INVALIDATING_LISTENERS) {
            // It doesn't really make sense to add listeners while we are invalidating them
            return null;
        }
        ListenerRemovalToken previous = listeners.put(listener, removalToken);
        if (previous == null) {
            bakeListeners();
        } else {
            assert previous == removalToken : "The same listener object must be registered with the same removal token";
        }
        return () -> {
            ListenerRemovalToken token = listeners.remove(listener);
            if (token != null) {
                assert token == removalToken;
                bakeListeners();
                removalToken.onListenerRemoved();
            }
        };
    }

    /** Sets the owner listener callback, which is never removed from the listener list when
     * {@link #invalidateListeners()} is called. */
    public void setOwnerListener(ItemInvAmountChangeListener ownerListener) {
        this.ownerListener = ownerListener;
    }

    private void bakeListeners() {
        bakedListeners = listeners.keySet().toArray(new ItemInvAmountChangeListener[0]);
    }

    public void invalidateListeners() {
        bakedListeners = INVALIDATING_LISTENERS;
        ListenerRemovalToken[] removalTokens = listeners.values().toArray(new ListenerRemovalToken[0]);
        listeners.clear();
        for (ListenerRemovalToken token : removalTokens) {
            token.onListenerRemoved();
        }
        bakedListeners = NO_LISTENERS;
    }

    protected final void fireAmountChange(ItemStack stack, int previous, int current) {
        if (ownerListener != null) {
            ownerListener.onChange(this, stack, previous, current);
        }
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final ItemInvAmountChangeListener[] baked = bakedListeners;
        for (ItemInvAmountChangeListener listener : baked) {
            listener.onChange(this, stack, previous, current);
        }
    }

    // NBT support
}
