/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.LimitedGroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.impl.SimpleLimitedGroupedFluidInv.ExtractionRule;
import alexiil.mc.lib.attributes.fluid.impl.SimpleLimitedGroupedFluidInv.InsertionRule;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class SimpleLimitedGroupedFluidInv extends DelegatingGroupedFluidInv implements LimitedGroupedFluidInv {

    private boolean isImmutable = false;

    // private final Map<FluidKey, ExactRule> exactRules = new HashMap<>();

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
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    @Override
    public FluidFilter getInsertionFilter() {
        // TODO Auto-generated method stub
        throw new AbstractMethodError("// TODO: Implement this!");
    }

    // Rules

    @Override
    public FluidLimitRule getRule(FluidFilter filter) {
        if (filter == ConstantFluidFilter.NOTHING) {
            return new FluidLimitRule() {
                // This filter affects nothing (why was it even called?)
                @Override
                public FluidLimitRule setMinimum(int min) {
                    return this;
                }

                @Override
                public FluidLimitRule limitInsertionCount(int max) {
                    return this;
                }
            };
        } else if (filter == ConstantFluidFilter.ANYTHING) {
            return new FluidLimitRule() {

                @Override
                public FluidLimitRule setMinimum(int min) {
                    extractionRules.clear();
                    if (min > 0) {
                        extractionRules.add(new ExtractionRule(filter, min));
                    }
                    return this;
                }

                @Override
                public FluidLimitRule limitInsertionCount(int max) {
                    insertionRules.clear();
                    if (max >= 0) {
                        insertionRules.add(new InsertionRule(filter, max));
                    }
                    return this;
                }
            };
        }
        // TODO: (Maybe?) Add filter decomposition for fluids
        return new FluidLimitRule() {
            @Override
            public FluidLimitRule setMinimum(int min) {
                extractionRules.add(new ExtractionRule(filter, min));
                return this;
            }

            @Override
            public FluidLimitRule limitInsertionCount(int max) {
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
        final int maximumInsertion;

        public InsertionRule(FluidFilter filter, int maximumInsertion) {
            this.filter = filter;
            this.maximumInsertion = maximumInsertion;
        }
    }

    static final class ExtractionRule {
        final FluidFilter filter;
        final int minimumAmount;

        public ExtractionRule(FluidFilter filter, int minimumAmount) {
            this.filter = filter;
            this.minimumAmount = minimumAmount;
        }
    }
}
