/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.ItemInvUtil;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.LimitedFixedItemInv;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/** A simple implementation of {@link LimitedFixedItemInv} that makes no assumptions about the backing
 * {@link FixedItemInv}. */
public class SimpleLimitedFixedItemInv extends DelegatingFixedItemInv implements LimitedFixedItemInv {

    private static final byte MAX_AMOUNT = (byte) 64;

    private final GroupedItemInv groupedItemInv;
    private boolean isImmutable = false;

    protected final ItemFilter[] insertionFilters;
    protected final byte[] maxInsertionAmounts;
    protected final byte[] minimumAmounts;

    public SimpleLimitedFixedItemInv(FixedItemInv delegate) {
        super(delegate);
        insertionFilters = new ItemFilter[delegate.getSlotCount()];
        maxInsertionAmounts = new byte[delegate.getSlotCount()];
        minimumAmounts = new byte[delegate.getSlotCount()];
        Arrays.fill(maxInsertionAmounts, MAX_AMOUNT);
        groupedItemInv = new DelegatingGroupedItemInv(delegate.getGroupedInv()) {
            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                if (maxAmount < 0) {
                    throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxAmount + ")");
                }
                ItemStack stack = ItemStack.EMPTY;
                if (maxAmount == 0) {
                    return stack;
                }
                FixedItemInv inv = SimpleLimitedFixedItemInv.this.delegate;
                for (int s = 0; s < getSlotCount(); s++) {
                    ItemStack slotStack = inv.getInvStack(s);
                    int minimum = minimumAmounts[s];
                    int available = slotStack.getCount() - minimum;
                    if (slotStack.isEmpty() || available <= 0) {
                        continue;
                    }
                    int slotMax = Math.min(maxAmount - stack.getCount(), available);
                    stack = ItemInvUtil.extractSingle(inv, s, filter, stack, slotMax, simulation);
                    if (stack.getCount() >= maxAmount) {
                        return stack;
                    }
                }
                return stack;
            }

            // No need to override attemptInsertion because:
            // - The maximum insertion amount is already available via FixedItemInv.getMaxAmount
            // - The item filter will already be followed via setInvStack.
        };
    }

    @Override
    public SimpleLimitedFixedItemInv markFinal() {
        isImmutable = true;
        return this;
    }

    protected void assertMutable() {
        if (isImmutable) {
            throw new IllegalStateException(
                "This object has already been marked as immutable, so no further changes are permitted!"
            );
        }
    }

    @Override
    public LimitedFixedItemInv copy() {
        SimpleLimitedFixedItemInv inv = new SimpleLimitedFixedItemInv(delegate);
        for (int i = 0; i < inv.getSlotCount(); i++) {
            inv.insertionFilters[i] = insertionFilters[i];
            inv.maxInsertionAmounts[i] = maxInsertionAmounts[i];
            inv.minimumAmounts[i] = minimumAmounts[i];
        }
        return inv;
    }

    // Overrides

    @Override
    public GroupedItemInv getGroupedInv() {
        return groupedItemInv;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getFilterForSlot(slot).matches(stack);
    }

    @Override
    public ItemFilter getFilterForSlot(int slot) {
        ItemFilter filter = insertionFilters[slot];
        if (filter != null) {
            return super.getFilterForSlot(slot).and(filter);
        }
        return super.getFilterForSlot(slot);
    }

    @Override
    public boolean setInvStack(int slot, ItemStack to, Simulation simulation) {
        ItemStack current = getInvStack(slot);
        boolean same = ItemStackUtil.areEqualIgnoreAmounts(current, to);
        boolean isExtracting = !same || to.getCount() < current.getCount();
        boolean isInserting = !same || to.getCount() > current.getCount();
        if (isExtracting) {
            if (same) {
                if (to.getCount() < minimumAmounts[slot]) {
                    return false;
                }
            } else {
                if (minimumAmounts[slot] > 0) {
                    return false;
                }
            }
        }
        if (isInserting) {
            if (!isItemValidForSlot(slot, to)) {
                return false;
            }
            if (to.getCount() > maxInsertionAmounts[slot]) {
                return false;
            }
        }
        return super.setInvStack(slot, to, simulation);
    }

    @Override
    public int getMaxAmount(int slot, ItemStack stack) {
        int ourMax = maxInsertionAmounts[slot];
        int delegateMax = super.getMaxAmount(slot, stack);
        return Math.min(ourMax, delegateMax);
    }

    // Rules

    @Override
    public ItemSlotLimitRule getRule(int slot) {
        return new ItemSlotLimitRule() {

            @Override
            public ItemSlotLimitRule setMinimum(int min) {
                if (min < 0 || min > 63) {
                    min = 0;
                }
                minimumAmounts[slot] = (byte) min;
                return this;
            }

            @Override
            public ItemSlotLimitRule limitInsertionCount(int max) {
                if (max < 0 || max > 63) {
                    max = MAX_AMOUNT;
                }
                maxInsertionAmounts[slot] = (byte) max;
                return this;
            }

            @Override
            public ItemSlotLimitRule filterInserts(ItemFilter filter) {
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                insertionFilters[slot] = filter;
                return this;
            }
        };
    }

    @Override
    public ItemSlotLimitRule getSubRule(int from, int to) {
        return new ItemSlotLimitRule() {

            @Override
            public ItemSlotLimitRule setMinimum(int min) {
                byte value;
                if (min < 0 || min > 63) {
                    value = 0;
                } else {
                    value = (byte) min;
                }
                Arrays.fill(minimumAmounts, from, to, value);
                return this;
            }

            @Override
            public ItemSlotLimitRule limitInsertionCount(int max) {
                byte value;
                if (max < 0 || max > 63) {
                    value = MAX_AMOUNT;
                } else {
                    value = (byte) max;
                }
                Arrays.fill(maxInsertionAmounts, from, to, value);
                return this;
            }

            @Override
            public ItemSlotLimitRule filterInserts(ItemFilter filter) {
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                Arrays.fill(insertionFilters, from, to, filter);
                return this;
            }
        };
    }
}
