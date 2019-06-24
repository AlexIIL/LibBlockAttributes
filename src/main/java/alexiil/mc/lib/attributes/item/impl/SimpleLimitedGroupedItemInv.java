/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.item.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.GroupedItemInv;
import alexiil.mc.lib.attributes.item.LimitedGroupedItemInv;
import alexiil.mc.lib.attributes.item.filter.ConstantItemFilter;
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
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    @Override
    public ItemStack attemptInsertion(ItemStack stack, Simulation simulation) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    @Override
    public ItemFilter getInsertionFilter() {
        // TODO: return a more useful filter!
        return stack -> {
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("You should never test an ItemFilter with an empty stack!");
            }
            ItemStack leftover = attemptInsertion(stack, Simulation.SIMULATE);
            return leftover.isEmpty() || leftover.getAmount() < stack.getAmount();
        };
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
