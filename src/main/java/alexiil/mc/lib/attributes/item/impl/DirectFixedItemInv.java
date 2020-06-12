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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv.ModifiableFixedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.Saveable;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple implementation of {@link ModifiableFixedItemInv} that supports all of the features that the interface
 * exposes. For simplicities sake this also implements {@link GroupedItemInv}, however none of the grouped methods run
 * in O(1). */

// TODO: Think about new class names!

// Should this get renamed to SimpleFixedItemInv and a new (deprecated) class DirectFixedItemInv extend that to maintain
// backwards compatibility? (As this is basically identical to how it behaved in 0.4.x)

public class DirectFixedItemInv implements ModifiableFixedItemInv, GroupedItemInv, Saveable {

    private static final InvMarkDirtyListener[] NO_LISTENERS = new InvMarkDirtyListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final InvMarkDirtyListener[] INVALIDATING_LISTENERS = new InvMarkDirtyListener[0];

    private final int slotCount;
    private final DefaultedList<ItemStack> slots;

    private int changes = 0;

    // TODO: Optimise this to cache more information!
    private final GroupedItemInv groupedVersion = new GroupedItemInvFixedWrapper(this);

    private final Map<InvMarkDirtyListener, ListenerRemovalToken> listeners
        = new Object2ObjectLinkedOpenCustomHashMap<>(Util.identityHashStrategy());

    private InvMarkDirtyListener ownerListener;

    private InvMarkDirtyListener[] bakedListeners = NO_LISTENERS;

    public DirectFixedItemInv(int slotCount) {
        this.slotCount = slotCount;
        this.slots = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);
    }

    // ##################
    //
    // Direct access
    //
    // ##################

    /** @deprecated This used to be necessary in 0.4.x, but since 0.5.0 {@link #getInvStack(int)} just returns the
     *             itemstack in the slot index. */
    @Deprecated
    public final ItemStack get(int slot) {
        return getInvStack(slot);
    }

    /** @deprecated This used to be necessary in 0.4.x, but since 0.5.0 this is unnecessary. */
    @Deprecated
    public final void set(int slot, ItemStack stack) {
        forceSetInvStack(slot, stack);
    }

    @Override
    public final void markDirty() {
        changes++;
        for (InvMarkDirtyListener listener : bakedListeners) {
            listener.onMarkDirty(this);
        }
    }

    /** Removes every listener currently registered to this inventory. */
    public final void invalidateListeners() {
        bakedListeners = INVALIDATING_LISTENERS;
        ListenerRemovalToken[] removalTokens = listeners.values().toArray(new ListenerRemovalToken[0]);
        listeners.clear();
        for (ListenerRemovalToken token : removalTokens) {
            token.onListenerRemoved();
        }
        bakedListeners = NO_LISTENERS;
        bakeListeners();
    }

    public void validateSlotIndex(int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IllegalArgumentException("Invalid slot index: " + slot + "(min = 0, max = " + slotCount + ")");
        }
    }

    // ##################
    //
    // NBT support
    //
    // ##################

    @Override
    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        ListTag tanksTag = new ListTag();
        for (ItemStack stack : slots) {
            tanksTag.add(stack.toTag(new CompoundTag()));
        }
        tag.put("slots", tanksTag);
        return tag;
    }

    @Override
    public void fromTag(CompoundTag tag) {
        ListTag slotsTag = tag.getList("slots", new CompoundTag().getType());
        for (int i = 0; i < slotsTag.size() && i < slots.size(); i++) {
            slots.set(i, ItemStack.fromTag(slotsTag.getCompound(i)));
        }
        for (int i = slotsTag.size(); i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }
    }

    // ##################
    //
    // Overrides
    //
    // ##################

    @Override
    public int getSlotCount() {
        return slotCount;
    }

    @Override
    public ItemStack getInvStack(int slot) {
        validateSlotIndex(slot);
        return slots.get(slot);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public final ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
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
    public void setOwnerListener(InvMarkDirtyListener ownerListener) {
        this.ownerListener = ownerListener;
        bakeListeners();
    }

    private void bakeListeners() {
        if (listeners.isEmpty() && ownerListener == null) {
            bakedListeners = NO_LISTENERS;
            return;
        }
        InvMarkDirtyListener[] array = listeners.keySet().toArray(new InvMarkDirtyListener[0]);
        if (ownerListener != null) {
            InvMarkDirtyListener[] array2 = new InvMarkDirtyListener[array.length + 1];
            System.arraycopy(array, 0, array2, 1, array.length);
            array2[0] = ownerListener;
            array = array2;
        }
        bakedListeners = array;
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        validateSlotIndex(slot);
        if (to != slots.get(slot) && !isItemValidForSlot(slot, to)) {
            return false;
        }
        if (simulation.isAction()) {
            slots.set(slot, to);
            markDirty();
        }
        return true;
    }

    @Override
    public int getChangeValue() {
        return changes;
    }

    // ##################
    //
    // GroupedItemInv
    //
    // ##################

    @Override
    public Set<ItemStack> getStoredStacks() {
        return groupedVersion.getStoredStacks();
    }

    @Override
    public int getTotalCapacity() {
        return groupedVersion.getTotalCapacity();
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        return groupedVersion.getStatistics(filter);
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        return groupedVersion.addListener(listener, removalToken);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return groupedVersion.attemptInsertion(stack, simulation);
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return groupedVersion.attemptExtraction(filter, maxAmount, simulation);
    }

    // #############################################
    //
    // Smaller methods for slot-specific insertion and extraction
    //
    // #############################################

    /** Splits off the given amount from the {@link ItemStack} in the given slot. */
    public ItemStack extract(int slot, int count) {
        ItemStack current = get(slot);
        ItemStack split = current.split(count);
        set(slot, current);
        return split;
    }

    /** Tries to insert the given stack into the given slot.
     * 
     * @return The result that couldn't be inserted. */
    public ItemStack insert(int slot, ItemStack stack) {
        // TODO: Optimise this!
        return ItemInvUtil.insertSingle(this, slot, stack, Simulation.ACTION);
    }
}
