/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.lang.reflect.Method;
import java.util.Map;

import alexiil.mc.lib.attributes.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.item.FixedItemInv.CopyingFixedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.ItemTransferable;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple, extendible, fixed size item inventory that supports all of the features that {@link CopyingFixedItemInv}
 * exposes.
 * <p>
 * Extending classes should take care to override {@link #getFilterForSlot(int)} if they also override
 * {@link #isItemValidForSlot(int, ItemStack)}. */
public class FullFixedItemInv implements CopyingFixedItemInv, ItemTransferable, Savable {

    private static final ItemInvSlotChangeListener[] NO_LISTENERS = new ItemInvSlotChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final ItemInvSlotChangeListener[] INVALIDATING_LISTENERS = new ItemInvSlotChangeListener[0];

    protected final DefaultedList<ItemStack> slots;

    // TODO: Optimise this to cache more information!
    private final GroupedItemInv groupedVersion = new GroupedItemInvFixedWrapper(this);

    private int changes = 0;
    private ItemInvSlotChangeListener ownerListener;

    private final Map<ItemInvSlotChangeListener, ListenerRemovalToken> listeners
        = new Object2ObjectLinkedOpenCustomHashMap<>(SystemUtil.identityHashStrategy());

    // Should this use WeakReference instead of storing them directly?
    private ItemInvSlotChangeListener[] bakedListeners = NO_LISTENERS;

    public FullFixedItemInv(int invSize) {
        slots = DefaultedList.ofSize(invSize, ItemStack.EMPTY);

        if (AttributeUtil.EXPENSIVE_DEBUG_CHECKS) {
            Class<?> cls = getClass();
            if (cls != FullFixedItemInv.class) {
                try {
                    Method method1 = cls.getMethod("isItemValidForSlot", int.class, ItemStack.class);
                    Method method2 = cls.getMethod("getFilterForSlot", int.class);
                    boolean overriden1 = method1.getDeclaringClass() != FullFixedItemInv.class;
                    boolean overriden2 = method2.getDeclaringClass() != FullFixedItemInv.class;
                    if (overriden1 != overriden2) {
                        // only one has been overridden, which probably isn't going to go well.
                        throw new IllegalStateException(
                            "The subclass " + method2.getDeclaringClass()
                                + " has overriden isItemValidForSlot() or getFilterForSlot() without overriding the other!"
                        );
                    }
                } catch (ReflectiveOperationException roe) {
                    throw new Error(
                        "Failed to get the isItemValidForSlot method! I'm not sure what"
                            + " to do now, as this shouldn't happen normally :(", roe
                    );
                }
            }
        }
    }

    @Override
    public final int getSlotCount() {
        return slots.size();
    }

    @Override
    public ItemStack getUnmodifiableInvStack(int slot) {
        ItemStack stack = slots.get(slot);
        ItemInvModificationTracker.trackNeverChanging(stack);
        return stack;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return true;
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        return ConstantItemFilter.ANYTHING;
    }

    @Override
    public GroupedItemInv getGroupedInv() {
        return this.groupedVersion;
    }

    @Override
    public int getChangeValue() {
        return changes;
    }

    @Override
    public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
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
    public void setOwnerListener(ItemInvSlotChangeListener ownerListener) {
        this.ownerListener = ownerListener;
    }

    private void bakeListeners() {
        bakedListeners = listeners.keySet().toArray(new ItemInvSlotChangeListener[0]);
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

    protected final void fireSlotChange(int slot, ItemStack previous, ItemStack current) {
        changes++;
        if (ownerListener != null) {
            ownerListener.onChange(this, slot, previous, current);
        }
        // Iterate over the previous array in case the listeners array is changed while we are iterating
        final ItemInvSlotChangeListener[] baked = bakedListeners;
        for (ItemInvSlotChangeListener listener : baked) {
            listener.onChange(this, slot, previous, current);
        }
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        boolean allowed = false;
        if (to.isEmpty()) {
            allowed = true;
        } else {
            ItemStack current = getInvStack(slot);
            if (
                !current.isEmpty() && current.getCount() > to.getCount() && ItemStackUtil.areEqualIgnoreAmounts(
                    to, current
                )
            ) {
                allowed = true;
            } else if (isItemValidForSlot(slot, to) && to.getCount() <= getMaxAmount(slot, to)) {
                allowed = true;
            }
        }
        if (allowed) {
            if (simulation == Simulation.ACTION) {
                ItemStack before = slots.get(slot);
                ItemInvModificationTracker.trackNeverChanging(before);
                ItemInvModificationTracker.trackNeverChanging(to);
                slots.set(slot, to);
                fireSlotChange(slot, before, to);
                ItemInvModificationTracker.trackNeverChanging(before);
                ItemInvModificationTracker.trackNeverChanging(to);
            }
            return true;
        }
        return false;
    }

    // NBT support
    public CompoundTag toTag(CompoundTag tag) {
        ListTag tanksTag = new ListTag();
        for (ItemStack stack : slots) {
            ItemInvModificationTracker.trackNeverChanging(stack);
            tanksTag.add(stack.toTag(new CompoundTag()));
        }
        tag.put("slots", tanksTag);
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        ListTag slotsTag = tag.getList("slots", new CompoundTag().getType());
        for (int i = 0; i < slotsTag.size() && i < slots.size(); i++) {
            slots.set(i, ItemStack.fromTag(slotsTag.getCompoundTag(i)));
        }
        for (int i = slotsTag.size(); i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }

    // ItemInsertable

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return groupedVersion.attemptInsertion(stack, simulation);
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return groupedVersion.getInsertionFilter();
    }

    // ItemExtractable

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return groupedVersion.attemptExtraction(filter, maxAmount, simulation);
    }

    @Override
    public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return groupedVersion.attemptAnyExtraction(maxAmount, simulation);
    }
}
