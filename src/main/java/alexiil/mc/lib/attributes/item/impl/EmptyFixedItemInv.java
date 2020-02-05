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
import alexiil.mc.lib.attributes.item.FixedItemInv.ModifiableFixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemTransferable;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** An {@link FixedItemInv} with no slots. Because this inventory is unmodifiable this also doubles as the empty
 * implementation for {@link FixedItemInvView}. */
public enum EmptyFixedItemInv implements ModifiableFixedItemInv, NullVariant {
    INSTANCE;

    private static IllegalArgumentException throwInvalidSlotException() {
        throw new IllegalArgumentException("There are no valid slots in this empty inventory!");
    }

    @Override
    public int getSlotCount() {
        return 0;
    }

    @Override
    public ItemStack getInvStack(int slot) {
        throw throwInvalidSlotException();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        throw throwInvalidSlotException();
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        throw throwInvalidSlotException();
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        throw throwInvalidSlotException();
    }

    @Override
    public int getChangeValue() {
        return 0;
    }

    @Override
    public void markDirty() {
        // NO-OP
    }

    @Override
    public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        throw throwInvalidSlotException();
    }

    @Override
    public FixedItemInvView getFixedView() {
        return this;
    }

    @Override
    public GroupedItemInv getGroupedInv() {
        return EmptyGroupedItemInv.INSTANCE;
    }

    @Override
    public ItemTransferable getTransferable() {
        return EmptyItemTransferable.NULL;
    }

    @Override
    public ItemInsertable getInsertable() {
        return RejectingItemInsertable.NULL;
    }

    @Override
    public ItemExtractable getExtractable() {
        return EmptyItemExtractable.NULL;
    }

    @Override
    public String toString() {
        return "EmptyFixedItemInv";
    }
}
