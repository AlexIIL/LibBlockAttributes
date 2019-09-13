/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.item.GroupedItemInvView;
import alexiil.mc.lib.attributes.item.ItemInvAmountChangeListener;
import alexiil.mc.lib.attributes.item.ItemStackCollections;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A combined version of multiple {@link GroupedItemInvView}'s. */
public class CombinedGroupedItemInvView implements GroupedItemInvView {

    final List<? extends GroupedItemInvView> inventories;

    public CombinedGroupedItemInvView(List<? extends GroupedItemInvView> inventories) {
        this.inventories = inventories;
    }

    @Override
    public int getAmount(ItemFilter filter) {
        int total = 0;
        for (GroupedItemInvView view : inventories) {
            total += view.getAmount(filter);
        }
        return total;
    }

    @Override
    public int getAmount(ItemStack stack) {
        int total = 0;
        for (GroupedItemInvView view : inventories) {
            total += view.getAmount(stack);
        }
        return total;
    }

    @Override
    public int getSpace(ItemStack stack) {
        int total = 0;
        for (GroupedItemInvView view : inventories) {
            total += view.getSpace(stack);
        }
        return total;
    }

    @Override
    public int getCapacity(ItemStack stack) {
        int total = 0;
        for (GroupedItemInvView view : inventories) {
            total += view.getCapacity(stack);
        }
        return total;
    }

    @Override
    public int getTotalCapacity() {
        int total = 0;
        for (GroupedItemInvView view : inventories) {
            total += view.getTotalCapacity();
        }
        return total;
    }

    @Override
    public ItemInvStatistic getStatistics(ItemFilter filter) {
        int amount = 0;
        int spaceAddable = 0;
        int spaceTotal = 0;
        for (GroupedItemInvView stats : inventories) {
            ItemInvStatistic stat = stats.getStatistics(filter);
            amount += stat.amount;
            spaceAddable += stat.spaceAddable;
            spaceTotal += stat.spaceTotal;
        }
        return new ItemInvStatistic(filter, amount, spaceAddable, spaceTotal);
    }

    @Override
    public Set<ItemStack> getStoredStacks() {
        Set<ItemStack> set = ItemStackCollections.set();
        for (GroupedItemInvView stats : inventories) {
            set.addAll(stats.getStoredStacks());
        }
        return set;
    }

    @Override
    public int getChangeValue() {
        int total = 0;
        for (GroupedItemInvView inv : inventories) {
            total += inv.getChangeValue();
        }
        return total;
    }

    @Override
    public ListenerToken addListener(ItemInvAmountChangeListener listener, ListenerRemovalToken removalToken) {
        final ListenerToken[] tokens = new ListenerToken[inventories.size()];
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
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = inventories.get(i).addListener((inv, stack, previous, current) -> {
                int totalCurrent = this.getAmount(stack);
                listener.onChange(this, stack, totalCurrent - current + previous, totalCurrent);
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
}
