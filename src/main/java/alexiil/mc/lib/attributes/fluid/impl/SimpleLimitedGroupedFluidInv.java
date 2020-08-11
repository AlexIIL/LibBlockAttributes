/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.LimitedGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ExactFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.SimpleLimitedGroupedFluidInv.ExtractionRule;
import alexiil.mc.lib.attributes.fluid.impl.SimpleLimitedGroupedFluidInv.InsertionRule;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class SimpleLimitedGroupedFluidInv extends DelegatingGroupedFluidInv implements LimitedGroupedFluidInv {

    private boolean isImmutable = false;

    private final List<InsertionRule> insertionRules = new ArrayList<>();
    private final List<ExtractionRule> extractionRules = new ArrayList<>();

    public SimpleLimitedGroupedFluidInv(GroupedFluidInv delegate) {
        super(delegate);
    }

    @Override
    public LimitedGroupedFluidInv markFinal() {
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
    public LimitedGroupedFluidInv copy() {
        SimpleLimitedGroupedFluidInv copy = new SimpleLimitedGroupedFluidInv(delegate);
        copy.extractionRules.addAll(this.extractionRules);
        copy.insertionRules.addAll(this.insertionRules);
        return copy;
    }

    // Overrides

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        if (filter == ConstantFluidFilter.NOTHING) {
            return FluidVolumeUtil.EMPTY;
        }
        if (extractionRules.isEmpty()) {
            return delegate.attemptExtraction(filter, maxAmount, simulation);
        }
        Set<FluidKey> keys = delegate.getStoredFluids();
        if (keys.isEmpty()) {
            return FluidVolumeUtil.EMPTY;
        }

        if (filter instanceof ExactFluidFilter) {
            FluidKey key = ((ExactFluidFilter) filter).fluid;
            if (!keys.contains(key)) {
                return FluidVolumeUtil.EMPTY;
            }
            keys = Collections.singleton(key);
        }

        fluids: for (FluidKey key : keys) {
            if (!filter.matches(key)) {
                continue;
            }
            FluidAmount current = delegate.getAmount_F(key);
            if (!current.isPositive()) {
                continue;
            }
            FluidAmount minLeft = FluidAmount.ZERO;
            for (ExtractionRule rule : extractionRules) {
                if (!rule.filter.matches(key)) {
                    continue;
                }
                if (rule.minimumAmount.isPositive()) {
                    minLeft = minLeft.max(rule.minimumAmount);
                    if (!current.isGreaterThan(minLeft)) {
                        continue fluids;
                    }
                }
            }
            FluidAmount allowed = current.sub(minLeft);
            return delegate.attemptExtraction(new ExactFluidFilter(key), maxAmount.min(allowed), simulation);
        }
        return FluidVolumeUtil.EMPTY;
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        if (fluid.isEmpty()) {
            return fluid;
        }
        FluidAmount current = delegate.getAmount_F(fluid.fluidKey);
        FluidAmount maxAmount = FluidAmount.MAX_BUCKETS;
        for (InsertionRule rule : insertionRules) {
            if (rule.filter.matches(fluid.fluidKey)) {
                maxAmount = maxAmount.min(rule.maximumInsertion);
                if (maxAmount.isLessThanOrEqual(current)) {
                    return fluid;
                }
            }
        }

        FluidAmount allowed = maxAmount.sub(current);
        assert allowed.isPositive();

        if (allowed.isLessThan(fluid.getAmount_F())) {
            FluidVolume original = fluid;
            FluidVolume offered = original.copy();
            FluidVolume leftover = delegate.attemptInsertion(offered.split(allowed), simulation);
            if (leftover.getAmount_F().equals(maxAmount)) {
                return original;
            }
            return FluidVolume.merge(offered, leftover);
        } else {
            return super.attemptInsertion(fluid, simulation);
        }
    }

    @Override
    public FluidFilter getInsertionFilter() {
        if (insertionRules.isEmpty()) {
            return delegate.getInsertionFilter();
        }
        List<FluidFilter> disallowed = new ArrayList<>();
        for (InsertionRule rule : insertionRules) {
            if (!rule.maximumInsertion.isPositive()) {
                disallowed.add(rule.filter);
            }
        }
        FluidFilter allowed = AggregateFluidFilter.anyOf(disallowed).negate();
        return allowed.and(delegate.getInsertionFilter());
    }

    // Rules

    @Override
    public FluidLimitRule getRule(FluidFilter filter) {
        if (filter == ConstantFluidFilter.NOTHING) {
            return new FluidLimitRule() {
                // This filter affects nothing (why was it even called?)
                @Override
                public FluidLimitRule setMinimum(FluidAmount min) {
                    return this;
                }

                @Override
                public FluidLimitRule limitInsertionAmount(FluidAmount max) {
                    return this;
                }
            };
        } else if (filter == ConstantFluidFilter.ANYTHING) {
            return new FluidLimitRule() {

                @Override
                public FluidLimitRule setMinimum(FluidAmount min) {
                    extractionRules.clear();
                    if (min.isPositive()) {
                        extractionRules.add(new ExtractionRule(filter, min));
                    }
                    return this;
                }

                @Override
                public FluidLimitRule limitInsertionAmount(FluidAmount max) {
                    insertionRules.clear();
                    if (!max.isNegative()) {
                        insertionRules.add(new InsertionRule(filter, max));
                    }
                    return this;
                }
            };
        }
        // TODO: (Maybe?) Add filter decomposition for fluids
        return new FluidLimitRule() {
            @Override
            public FluidLimitRule setMinimum(FluidAmount min) {
                extractionRules.add(new ExtractionRule(filter, min));
                return this;
            }

            @Override
            public FluidLimitRule limitInsertionAmount(FluidAmount max) {
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
        final FluidFilter filter;
        final FluidAmount maximumInsertion;

        public InsertionRule(FluidFilter filter, FluidAmount maximumInsertion) {
            this.filter = filter;
            this.maximumInsertion = maximumInsertion;
        }
    }

    static final class ExtractionRule {
        final FluidFilter filter;
        final FluidAmount minimumAmount;

        public ExtractionRule(FluidFilter filter, FluidAmount minimumAmount) {
            this.filter = filter;
            this.minimumAmount = minimumAmount;
        }
    }
}
