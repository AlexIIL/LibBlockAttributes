/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Collections;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemExtractable;
import alexiil.mc.lib.attributes.item.ItemInsertable;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** {@link GroupedItemInvView} for an empty inventory. */
public enum EmptyGroupedItemInv implements GroupedItemInv, NullVariant {
    INSTANCE;

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        return new ItemInvStatistic(filter, 0, 0, 0);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return Collections.emptySet();
    }

    @Override
    public int getAmount(ItemFilter filter) {
        return 0;
    }

    @Override
    public int getAmount(ItemStack stack) {
        return 0;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return 0;
    }

    @Override
    public int getSpace(ItemStack stack) {
        return 0;
    }

    @Override
    public int getTotalCapacity() {
        return 0;
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return ConstantItemFilter.NOTHING;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return stack;
    }

    @Override
    public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return ItemStack.EMPTY;
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }

    @Override
    public int getChangeValue() {
        return 0;
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
    public GroupedItemInvView getGroupedView() {
        return this;
    }

    @Override
    public ItemInsertable getPureInsertable() {
        return RejectingItemInsertable.NULL;
    }

    @Override
    public ItemExtractable getPureExtractable() {
        return EmptyItemExtractable.NULL;
    }

    @Override
    public String toString() {
        return "EmptyGroupedItemInv";
    }
}
