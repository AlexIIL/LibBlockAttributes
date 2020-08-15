/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.math.RoundingMode;
import java.util.Arrays;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.LimitedFixedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class SimpleLimitedFixedFluidInv extends DelegatingFixedFluidInv implements LimitedFixedFluidInv {

    private final GroupedFluidInv groupedInv;
    private boolean isImmutable = false;

    protected final FluidFilter[] insertionFilters;
    protected final FluidAmount[] maxInsertionAmounts;
    protected final FluidAmount[] minimumAmounts;

    public SimpleLimitedFixedFluidInv(FixedFluidInv delegate) {
        super(delegate);

        insertionFilters = new FluidFilter[getTankCount()];
        maxInsertionAmounts = new FluidAmount[getTankCount()];
        minimumAmounts = new FluidAmount[getTankCount()];
        groupedInv = new DelegatingGroupedFluidInv(super.getGroupedInv()) {
            @Override
            public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
                if (maxAmount.isNegative()) {
                    throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxAmount + ")");
                }
                FluidVolume volume = FluidVolumeUtil.EMPTY;
                if (maxAmount.isZero()) {
                    return volume;
                }
                FixedFluidInv inv = SimpleLimitedFixedFluidInv.this;
                for (int t = 0; t < getTankCount(); t++) {
                    FluidVolume tankVolume = inv.getInvFluid(t);
                    FluidAmount minimum = minimumAmounts[t];
                    FluidAmount available = tankVolume.getAmount_F().roundedSub(minimum, RoundingMode.DOWN);
                    if (tankVolume.isEmpty() || !available.isPositive()) {
                        continue;
                    }
                    FluidAmount tankMax = maxAmount.roundedSub(volume.getAmount_F(), RoundingMode.DOWN).min(available);
                    volume = FluidVolumeUtil.extractSingle(inv, t, filter, volume, tankMax, simulation);
                    if (!volume.getAmount_F().isLessThan(maxAmount)) {
                        return volume;
                    }
                }
                return volume;
            }
            // No need to override attemptInsertion because:
            // - The maximum insertion amount is already available via FixedFluidInv.getMaxAmount
            // - The fluid filter will already be followed via setInvFluid.
        };
    }

    @Override
    public LimitedFixedFluidInv markFinal() {
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
    public LimitedFixedFluidInv copy() {
        SimpleLimitedFixedFluidInv copy = new SimpleLimitedFixedFluidInv(delegate);
        System.arraycopy(insertionFilters, 0, copy.insertionFilters, 0, getTankCount());
        System.arraycopy(maxInsertionAmounts, 0, copy.maxInsertionAmounts, 0, getTankCount());
        System.arraycopy(minimumAmounts, 0, copy.minimumAmounts, 0, getTankCount());
        return copy;
    }

    // Overrides

    @Override
    public GroupedFluidInv getGroupedInv() {
        return groupedInv;
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return getFilterForTank(tank).matches(fluid);
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        FluidFilter filter = insertionFilters[tank];
        if (filter != null) {
            return super.getFilterForTank(tank).and(filter);
        }
        return super.getFilterForTank(tank);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        FluidVolume current = getInvFluid(tank);
        boolean same = current.getFluidKey().equals(to.fluidKey);
        boolean isExtracting = !current.isEmpty() && (!same || to.getAmount_F().isLessThan(current.getAmount_F()));
        boolean isInserting = !to.isEmpty() && (!same || to.getAmount_F().isGreaterThan(current.getAmount_F()));

        if (isExtracting) {
            if (same) {
                if (to.getAmount_F().isLessThan(minimumAmounts[tank])) {
                    return false;
                }
            } else {
                if (minimumAmounts[tank] != null) {
                    return false;
                }
            }
        }

        if (isInserting) {
            if (!isFluidValidForTank(tank, to.getFluidKey())) {
                return false;
            }
            if (to.getAmount_F().isGreaterThan(maxInsertionAmounts[tank])) {
                return false;
            }
        }
        return super.setInvFluid(tank, to, simulation);
    }

    @Override
    public FluidAmount getMaxAmount_F(int tank) {
        return super.getMaxAmount_F(tank).min(maxInsertionAmounts[tank]);
    }

    // Rules

    @Override
    public FluidTankLimitRule getRule(int tank) {
        return new FluidTankLimitRule() {

            @Override
            public FluidTankLimitRule setMinimum(FluidAmount min) {
                if (min != null && !min.isPositive()) {
                    min = null;
                }
                minimumAmounts[tank] = min;
                return this;
            }

            @Override
            public FluidTankLimitRule limitInsertionAmount(FluidAmount max) {
                if (max != null && !max.isPositive()) {
                    max = FluidAmount.MAX_BUCKETS;
                }
                maxInsertionAmounts[tank] = max;
                return this;
            }

            @Override
            public FluidTankLimitRule filterInserts(FluidFilter filter) {
                if (filter == ConstantFluidFilter.ANYTHING) {
                    filter = null;
                }
                insertionFilters[tank] = filter;
                return this;
            }
        };
    }

    @Override
    public FluidTankLimitRule getSubRule(int from, int to) {
        return new FluidTankLimitRule() {

            @Override
            public FluidTankLimitRule setMinimum(FluidAmount min) {
                if (min != null && !min.isPositive()) {
                    min = null;
                }
                Arrays.fill(minimumAmounts, min);
                return this;
            }

            @Override
            public FluidTankLimitRule limitInsertionAmount(FluidAmount max) {
                if (max != null && !max.isPositive()) {
                    max = FluidAmount.MAX_BUCKETS;
                }
                Arrays.fill(maxInsertionAmounts, max);
                return this;
            }

            @Override
            public FluidTankLimitRule filterInserts(FluidFilter filter) {
                if (filter == ConstantFluidFilter.ANYTHING) {
                    filter = null;
                }
                Arrays.fill(insertionFilters, filter);
                return this;
            }
        };
    }
}
