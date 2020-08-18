/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
import alexiil.mc.lib.attributes.item.ItemStackUtil;
import alexiil.mc.lib.attributes.item.LimitedFixedItemInv;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

/**
 * A simple implementation of {@link LimitedFixedItemInv} that makes no assumptions about the backing
 * {@link FixedItemInv}.
 */
public class SimpleLimitedFixedItemInv extends DelegatingFixedItemInv implements LimitedFixedItemInv {

    private static final byte MAX_AMOUNT = (byte) 64;

    private boolean isImmutable = false;

    protected final ItemFilter[] insertionFilters;
    protected final ItemFilter[] extractionFilters;
    protected final byte[] maxInsertionAmounts;
    protected final byte[] minimumAmounts;

    public SimpleLimitedFixedItemInv(FixedItemInv delegate) {
        super(delegate);
        insertionFilters = new ItemFilter[delegate.getSlotCount()];
        extractionFilters = new ItemFilter[delegate.getSlotCount()];
        maxInsertionAmounts = new byte[delegate.getSlotCount()];
        minimumAmounts = new byte[delegate.getSlotCount()];
        Arrays.fill(maxInsertionAmounts, MAX_AMOUNT);
    }

    public static SimpleLimitedFixedItemInv createLimited(FixedItemInv inv) {
        if (inv instanceof OfModifiable) {
            return new OfModifiable((ModifiableFixedItemInv) inv);
        }
        if (inv instanceof OfCopying) {
            return new OfCopying((CopyingFixedItemInv) inv);
        }
        return new SimpleLimitedFixedItemInv(inv);
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
        final SimpleLimitedFixedItemInv inv;
        // Test this class so we don't have any unexpected surprises if the returned class is different
        if (this instanceof OfModifiable) {
            inv = new OfModifiable((ModifiableFixedItemInv) delegate);
        } else if (this instanceof OfCopying) {
            inv = new OfCopying((CopyingFixedItemInv) delegate);
        } else {
            inv = new SimpleLimitedFixedItemInv(delegate);
        }
        inv.isImmutable = isImmutable;
        for (int i = 0; i < inv.getSlotCount(); i++) {
            inv.insertionFilters[i] = insertionFilters[i];
            inv.maxInsertionAmounts[i] = maxInsertionAmounts[i];
            inv.minimumAmounts[i] = minimumAmounts[i];
        }
        return inv;
    }

    // Overrides

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return (stack.isEmpty() || getFilterForSlot(slot).matches(stack)) && delegate.isItemValidForSlot(slot, stack);
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
        boolean isExtracting = !current.isEmpty() && (!same || to.getCount() < current.getCount());
        boolean isInserting = !to.isEmpty() && (!same || to.getCount() > current.getCount());
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
            if (extractionFilters[slot] != null && !extractionFilters[slot].matches(current)) {
                return false;
            }
        }
        if (isInserting) {
            if (to.getCount() > maxInsertionAmounts[slot]) {
                return false;
            }
            if (!isItemValidForSlot(slot, to)) {
                return false;
            }
        }
        return super.setInvStack(slot, to, simulation);
    }

    @Override
    public ItemStack insertStack(int slot, ItemStack stack, Simulation simulation) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Calculate the final amount after the insert operation, ignoring stacking and other rules here
        // since these will be handled by the delegate
        ItemStack current = getInvStack(slot);
        int currentCount = current.isEmpty() ? 0 : current.getCount();
        if (currentCount >= maxInsertionAmounts[slot]) {
            return stack; // Avoid a copy for the simple case
        }

        // Test the insertion filter before potentially copying
        if (insertionFilters[slot] != null && !insertionFilters[slot].matches(stack)) {
            return stack;
        }

        // If current count + added count exceed the maximum, we need to try and insert
        // a partial amount and adjust the excess
        int cannotAddAmount = Math.max(0, currentCount + stack.getCount() - maxInsertionAmounts[slot]);
        ItemStack cannotAdd = null;
        if (cannotAddAmount > 0) {
            stack = stack.copy();
            cannotAdd = stack.split(cannotAddAmount);
        }

        ItemStack excess = super.insertStack(slot, stack, simulation);

        // Now adjust the excess for what we held back due to the max-amount
        if (cannotAdd != null) {
            if (!excess.isEmpty()) {
                // The underlying inventory could _still_ reject some part of our insert attempt
                // Also note that we're not checking that excess is actually the same item here
                cannotAdd.increment(excess.getCount());
            }
            return cannotAdd;
        }
        return excess;
    }

    @Override
    public ItemStack extractStack(
            int slot, @Nullable ItemFilter filter, ItemStack mergeWith, int maxCount, Simulation simulation
    ) {
        ItemStack current = getInvStack(slot);
        if (current.isEmpty()) {
            return mergeWith;
        }

        maxCount = Math.min(maxCount, current.getCount() - minimumAmounts[slot]);
        if (maxCount <= 0) {
            return mergeWith;
        }

        if (extractionFilters[slot] != null && !extractionFilters[slot].matches(current)) {
            return mergeWith;
        }

        return super.extractStack(slot, filter, mergeWith, maxCount, simulation);
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
                assertMutable();
                minimumAmounts[slot] = (byte) Math.max(0, Math.min(MAX_AMOUNT, min));
                return this;
            }

            @Override
            public ItemSlotLimitRule limitInsertionCount(int max) {
                assertMutable();
                maxInsertionAmounts[slot] = (byte) Math.max(0, Math.min(MAX_AMOUNT, max));
                return this;
            }

            @Override
            public ItemSlotLimitRule filterInserts(ItemFilter filter) {
                assertMutable();
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                insertionFilters[slot] = filter;
                return this;
            }

            @Override
            public ItemSlotLimitRule filterExtracts(ItemFilter filter) {
                assertMutable();
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                extractionFilters[slot] = filter;
                return this;
            }
        };
    }

    @Override
    public ItemSlotLimitRule getSubRule(int from, int to) {
        return new ItemSlotLimitRule() {

            @Override
            public ItemSlotLimitRule setMinimum(int min) {
                assertMutable();
                byte value = (byte) Math.max(0, Math.min(MAX_AMOUNT, min));
                Arrays.fill(minimumAmounts, from, to, value);
                return this;
            }

            @Override
            public ItemSlotLimitRule limitInsertionCount(int max) {
                assertMutable();
                byte value = (byte) Math.max(0, Math.min(MAX_AMOUNT, max));
                Arrays.fill(maxInsertionAmounts, from, to, value);
                return this;
            }

            @Override
            public ItemSlotLimitRule filterInserts(ItemFilter filter) {
                assertMutable();
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                Arrays.fill(insertionFilters, from, to, filter);
                return this;
            }

            @Override
            public ItemSlotLimitRule filterExtracts(ItemFilter filter) {
                assertMutable();
                if (filter == ConstantItemFilter.ANYTHING) {
                    filter = null;
                }
                Arrays.fill(extractionFilters, from, to, filter);
                return this;
            }
        };
    }

    public static class OfModifiable extends SimpleLimitedFixedItemInv implements ModifiableFixedItemInv {

        public OfModifiable(ModifiableFixedItemInv delegate) {
            super(delegate);
        }

        @Override
        public void markDirty() {
            ((ModifiableFixedItemInv) delegate).markDirty();
        }

        @Override
        public ListenerToken addListener(InvMarkDirtyListener listener, ListenerRemovalToken removalToken) {
            ModifiableFixedItemInv wrapper = this;
            return ((ModifiableFixedItemInv) delegate).addListener(inv -> {
                listener.onMarkDirty(wrapper);
            }, removalToken);
        }
    }

    public static class OfCopying extends SimpleLimitedFixedItemInv implements CopyingFixedItemInv {

        public OfCopying(CopyingFixedItemInv delegate) {
            super(delegate);
        }

        @Override
        public ItemStack getUnmodifiableInvStack(int slot) {
            return ((CopyingFixedItemInv) delegate).getUnmodifiableInvStack(slot);
        }

        @Override
        public ListenerToken addListener(ItemInvSlotChangeListener listener, ListenerRemovalToken removalToken) {
            FixedItemInvView wrapper = this;
            return ((CopyingFixedItemInv) delegate).addListener((realInv, slot, previous, current) -> {
                listener.onChange(wrapper, slot, previous, current);
            }, removalToken);
        }
    }
}
