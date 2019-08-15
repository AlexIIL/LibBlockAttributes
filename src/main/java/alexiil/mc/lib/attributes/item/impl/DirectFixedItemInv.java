/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.SystemUtil;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener.ItemInvSlotListener;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;

/** A simple, directly modifiable, {@link FixedItemInv}. Unlike {@link SimpleFixedItemInv} this doesn't allow for
 * controlling the functionality quite as much.
 * <p>
 * If no listeners are registered to this inventory then this will perform slightly better than
 * {@link SimpleFixedItemInv}, (although the opposite is true when listeners are registered to this inventory, as this
 * needs to perform a bit more work in {@link #markDirty()}. */
public class DirectFixedItemInv implements FixedItemInv, GroupedItemInv {

    private static final ItemInvSlotChangeListener[] NO_LISTENERS = new ItemInvSlotChangeListener[0];

    /** Sentinel value used during {@link #invalidateListeners()}. */
    private static final ItemInvSlotChangeListener[] INVALIDATING_LISTENERS = new ItemInvSlotChangeListener[0];

    private final int slotCount;
    private final DefaultedList<ItemStack> slots;
    /** Only populated if a listener is actually registered that wants the previous stacks. */
    private final DefaultedList<ItemStack> lastSeenStacks;
    private final BitSet touchedSlots;

    // TODO: Optimise this to cache more information!
    private final GroupedItemInv groupedVersion = new GroupedItemInvFixedWrapper(this);

    private final Map<ItemInvSlotChangeListener, ListenerRemovalToken> listeners
        = new Object2ObjectLinkedOpenCustomHashMap<>(SystemUtil.identityHashStrategy());

    private ItemInvSlotChangeListener ownerListener;

    private ItemInvSlotChangeListener[] bakedListeners = NO_LISTENERS;

    // Optimisation flag: this allows us to avoid copying if we don't have listeners that need it.
    /** Set to false if {@link #listeners} only contains instances of {@link ItemInvSlotListener}, or it's empty. */
    private boolean listenersNeedStacks = false;

    public DirectFixedItemInv(int slotCount) {
        this.slotCount = slotCount;
        this.slots = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);
        this.lastSeenStacks = DefaultedList.ofSize(slotCount, ItemStack.EMPTY);
        this.touchedSlots = new BitSet(slotCount);
    }

    // ##################
    //
    // Direct access
    //
    // ##################

    /** Gets the {@link ItemStack} stored in the given slot.
     * 
     * @return The {@link ItemStack} in the slot. Unlike {@link #getInvStack(int)} you <strong>are allowed to
     *         modify</strong> the returned stack! */
    public final ItemStack get(int slot) {
        validateSlotIndex(slot);
        touchedSlots.set(slot);
        return slots.get(slot);
    }

    /** Directly sets the stack in the given slot, ignoring any filters that may be present.
     * 
     * @param slot
     * @param stack */
    public final void set(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        touchedSlots.clear(slot);
        slots.set(slot, stack);
        if (listenersNeedStacks) {
            ItemStack previous = lastSeenStacks.get(slot);
            ItemStack current = stack.copy();
            lastSeenStacks.set(slot, current);

            // Even though this inventory provides mutable stacks the listeners still expect non-modifiable stacks
            ItemInvModificationTracker.trackNeverChanging(previous);
            ItemInvModificationTracker.trackNeverChanging(current);
            for (ItemInvSlotChangeListener listener : bakedListeners) {
                listener.onChange(this, slot, previous, current);
            }
            ItemInvModificationTracker.trackNeverChanging(previous);
            ItemInvModificationTracker.trackNeverChanging(current);
        } else {
            for (ItemInvSlotChangeListener listener : bakedListeners) {
                ((ItemInvSlotListener) listener).onChange(this, slot);
            }
        }
    }

    /** Call this if you modify the result of {@link #get(int)} without calling {@link #set(int, ItemStack)}
     * afterwards! */
    public final void markDirty() {
        for (int i = touchedSlots.nextSetBit(0); i >= 0; i = touchedSlots.nextSetBit(i + 1)) {
            touchedSlots.clear(i);
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }

            if (listenersNeedStacks) {
                ItemStack previous = lastSeenStacks.get(i);
                ItemStack current = slots.get(i).copy();
                lastSeenStacks.set(i, current);

                // Even though this inventory provides mutable stacks the listeners still expect non-modifiable stacks
                ItemInvModificationTracker.trackNeverChanging(previous);
                ItemInvModificationTracker.trackNeverChanging(current);
                for (ItemInvSlotChangeListener listener : bakedListeners) {
                    listener.onChange(this, i, previous, current);
                }
                ItemInvModificationTracker.trackNeverChanging(previous);
                ItemInvModificationTracker.trackNeverChanging(current);
            } else {
                for (ItemInvSlotChangeListener listener : bakedListeners) {
                    ((ItemInvSlotListener) listener).onChange(this, i);
                }
            }
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

    public final CompoundTag toTag() {
        return toTag(new CompoundTag());
    }

    public CompoundTag toTag(CompoundTag tag) {
        ListTag tanksTag = new ListTag();
        for (ItemStack stack : slots) {
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

    // ##################
    //
    // Overrides
    //
    // ##################

    @Override
    public int getSlotCount() {
        return slotCount;
    }

    /** @return A copy of the {@link ItemStack} in the given slot.
     * @deprecated Because you probably want to use {@link #get(int)} instead. */
    @Override
    @Deprecated
    public ItemStack getInvStack(int slot) {
        return slots.get(slot).copy();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public final ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
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
        bakeListeners();
    }

    private void bakeListeners() {
        boolean didNeedStacks = listenersNeedStacks;
        listenersNeedStacks = false;
        if (listeners.isEmpty() && ownerListener == null) {
            bakedListeners = NO_LISTENERS;
            return;
        }
        ItemInvSlotChangeListener[] array = listeners.keySet().toArray(new ItemInvSlotChangeListener[0]);
        if (ownerListener != null) {
            ItemInvSlotChangeListener[] array2 = new ItemInvSlotChangeListener[array.length + 1];
            System.arraycopy(array, 0, array2, 1, array.length);
            array2[0] = ownerListener;
            array = array2;
        }
        for (ItemInvSlotChangeListener listener : array) {
            if (!(listener instanceof ItemInvSlotListener)) {
                listenersNeedStacks = true;
                break;
            }
        }
        bakedListeners = array;

        if (!didNeedStacks && listenersNeedStacks) {
            for (int i = 0; i < slotCount; i++) {
                lastSeenStacks.set(i, slots.get(i).copy());
            }
        }
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        validateSlotIndex(slot);
        if (!isItemValidForSlot(slot, to)) {
            return false;
        }
        if (simulation.isAction()) {
            set(slot, to.copy());
        }
        return true;
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
     * @return The leftover that couldn't be inserted. */
    public ItemStack insert(int slot, ItemStack stack) {
        // TODO: Optimise this!
        return ItemInvUtil.insertSingle(this, slot, stack, Simulation.ACTION);
    }
}
