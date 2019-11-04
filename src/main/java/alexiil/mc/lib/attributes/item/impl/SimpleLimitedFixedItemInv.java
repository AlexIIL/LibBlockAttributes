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

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInv;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener;
import alexiil.mc.lib.attributes.item.ItemInvSlotChangeListener;
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
        groupedItemInv = new DelegatingGroupedItemInv(super.getGroupedInv()) {
            @Override
            public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
                if (maxAmount < 0) {
                    throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxAmount + ")");
                }
                ItemStack stack = ItemStack.EMPTY;
                if (maxAmount == 0) {
                    return stack;
                }
                FixedItemInv inv = SimpleLimitedFixedItemInv.this;
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
