/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.LimitedGroupedItemInv;
import alexiil.mc.lib.attributes.item.filter.AggregateItemFilter;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
import alexiil.mc.lib.attributes.item.filter.ExactItemStackFilter;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;

public class SimpleLimitedGroupedItemInv extends DelegatingGroupedItemInv implements LimitedGroupedItemInv {

    private boolean isImmutable = false;

    // private final Map<Item, ExactRule> exactItemRules = new HashMap<>();
    // private final Map<ItemStack, ExactRule> exactStackRules = ItemStackCollections.map();

    private final List<InsertionRule> insertionRules = new ArrayList<>();
    private final List<ExtractionRule> extractionRules = new ArrayList<>();

    public SimpleLimitedGroupedItemInv(GroupedItemInv delegate) {
        super(delegate);
    }

    @Override
    public LimitedGroupedItemInv markFinal() {
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
    public LimitedGroupedItemInv copy() {
        SimpleLimitedGroupedItemInv copy = new SimpleLimitedGroupedItemInv(delegate);
        copy.extractionRules.addAll(this.extractionRules);
        copy.insertionRules.addAll(this.insertionRules);
        return copy;
    }

    // Overrides

    @Override
    public ItemStack attemptExtraction(ItemFilter filter, int maxAmount, Simulation simulation) {
        if (filter == ConstantItemFilter.NOTHING || filter == ConstantItemFilter.ONLY_EMPTY) {
            return ItemStack.EMPTY;
        }
        if (extractionRules.isEmpty()) {
            return delegate.attemptExtraction(filter, maxAmount, simulation);
        }
        Set<ItemStack> stacks = delegate.getStoredStacks();
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (filter instanceof ExactItemStackFilter) {
            ItemStack stack = ((ExactItemStackFilter) filter).stack;
            if (!stacks.contains(stack)) {
                return ItemStack.EMPTY;
            }
            stacks = Collections.singleton(stack);
        }

        fluids: for (ItemStack stack : stacks) {
            if (!filter.matches(stack)) {
                continue;
            }
            int current = delegate.getAmount(stack);
            if (current <= 0) {
                continue;
            }
            int minLeft = 0;
            for (ExtractionRule rule : extractionRules) {
                if (!rule.filter.matches(stack)) {
                    continue;
                }
                if (rule.minimumAmount > 0) {
                    minLeft = Math.max(minLeft, rule.minimumAmount);
                    if (current <= minLeft/* !current.isGreaterThan(minLeft)*/) {
                        continue fluids;
                    }
                }
            }
            int allowed = current - minLeft;
            return delegate
                .attemptExtraction(new ExactItemStackFilter(stack), Math.min(maxAmount, allowed), simulation);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        if (stack.isEmpty()) {
            return stack;
        }
        int current = delegate.getAmount(stack);
        int maxCount = Integer.MAX_VALUE;
        for (InsertionRule rule : insertionRules) {
            if (rule.filter.matches(stack)) {
                maxCount = Math.min(maxCount, rule.maximumInsertion);
                if (maxCount <= current) {
                    return stack;
                }
            }
        }

        int allowed = maxCount - current;
        assert allowed >= 1;

        if (allowed < stack.getCount()) {
            ItemStack original = stack;
            ItemStack offered = original.copy();
            ItemStack leftover = delegate.attemptInsertion(offered.split(allowed), simulation);
            if (leftover.getCount() == maxCount) {
                return original;
            }
            offered.increment(leftover.getCount());
            return offered;
        } else {
            return super.attemptInsertion(stack, simulation);
        }
    }

    @Override
    public ItemFilter getInsertionFilter() {
        if (insertionRules.isEmpty()) {
            return delegate.getInsertionFilter();
        }
        List<ItemFilter> disallowed = new ArrayList<>();
        for (InsertionRule rule : insertionRules) {
            if (rule.maximumInsertion <= 0) {
                disallowed.add(rule.filter);
            }
        }
        ItemFilter allowed = AggregateItemFilter.anyOf(disallowed).negate();
        return allowed.and(delegate.getInsertionFilter());
    }

    // Rules

    @Override
    public ItemLimitRule getRule(ItemFilter filter) {
        if (filter == ConstantItemFilter.NOTHING) {
            return new ItemLimitRule() {
                // This filter affects nothing (why was it even called?)
                @Override
                public ItemLimitRule setMinimum(int min) {
                    return this;
                }

                @Override
                public ItemLimitRule limitInsertionCount(int max) {
                    return this;
                }
            };
        } else if (filter == ConstantItemFilter.ANYTHING) {
            return new ItemLimitRule() {
                @Override
                public ItemLimitRule setMinimum(int min) {
                    extractionRules.clear();
                    if (min > 0) {
                        extractionRules.add(new ExtractionRule(filter, min));
                    }
                    return this;
                }

                @Override
                public ItemLimitRule limitInsertionCount(int max) {
                    insertionRules.clear();
                    if (max >= 0) {
                        insertionRules.add(new InsertionRule(filter, max));
                    }
                    return this;
                }
            };
        }
        // TODO: (Maybe?) Add filter decomposition for items and stacks
        return new ItemLimitRule() {
            @Override
            public ItemLimitRule setMinimum(int min) {
                extractionRules.add(new ExtractionRule(filter, min));
                return this;
            }

            @Override
            public ItemLimitRule limitInsertionCount(int max) {
                insertionRules.add(new InsertionRule(filter, max));
                return this;
            }
        };
    }

    // static final class ExactRule {
    // final int maximumInsertion;
    // final int minumAmount;
    //
    // public ExactRule(int maximumInsertion, int minumAmount) {
    // this.maximumInsertion = maximumInsertion;
    // this.minumAmount = minumAmount;
    // }
    // }

    static final class InsertionRule {
        final ItemFilter filter;
        final int maximumInsertion;

        public InsertionRule(ItemFilter filter, int maximumInsertion) {
            this.filter = filter;
            this.maximumInsertion = maximumInsertion;
        }
    }

    static final class ExtractionRule {
        final ItemFilter filter;
        final int minimumAmount;

        public ExtractionRule(ItemFilter filter, int minimumAmount) {
            this.filter = filter;
            this.minimumAmount = minimumAmount;
        }
    }
}
