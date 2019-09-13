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
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class DelegatingGroupedItemInv implements GroupedItemInv {

    protected final GroupedItemInv delegate;

    public DelegatingGroupedItemInv(GroupedItemInv delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        return Collections.unmodifiableSet(delegate.getStoredStacks());
    }

    @Override
    public int getTotalCapacity() {
        return delegate.getTotalCapacity();
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        return delegate.getStatistics(filter);
    }

    @Override
    public int getChangeValue() {
        return delegate.getChangeValue();
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        GroupedItemInv real = this;
        return delegate.addListener((inv, stack, previous, current) -> {
            listener.onChange(real, stack, previous, current);
        }, removalToken);
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        return delegate.attemptInsertion(stack, simulation);
    }

    @Override
    public ItemFilter getInsertionFilter() {
        return delegate.getInsertionFilter();
    }

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        return delegate.attemptExtraction(filter, maxAmount, simulation);
    }

    @Override
    public ItemStack attemptAnyExtraction(int maxAmount, Simulation simulation) {
        return delegate.attemptAnyExtraction(maxAmount, simulation);
    }
}
