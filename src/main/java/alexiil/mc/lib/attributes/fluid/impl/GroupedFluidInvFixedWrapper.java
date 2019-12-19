/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** An {@link GroupedFluidInv} wrapper over an {@link FixedFluidInv}. This implementation is the naive implementation
 * where every insertion operation will look at every tank in the target inventory in order to insert into the most
 * appropriate tank first. As such the use of this class is discouraged whenever a more efficient version can be made
 * (unless the target inventory has a very small {@link FixedFluidInvView#getTankCount() size}. */
public class GroupedFluidInvFixedWrapper extends GroupedFluidInvViewFixedWrapper implements GroupedFluidInv {

    public GroupedFluidInvFixedWrapper(FixedFluidInv inv) {
        super(inv);
    }

    @Override
    protected FixedFluidInv inv() {
        return (FixedFluidInv) super.inv();
    }

    @Override
    public FluidFilter getInsertionFilter() {
        int tankCount = inv().getTankCount();
        switch (tankCount) {
            case 0: {
                // What?
                return ConstantFluidFilter.NOTHING;
            }
            case 1: {
                return inv().getFilterForTank(0);
            }
            case 2: {
                return inv().getFilterForTank(0).and(inv().getFilterForTank(1));
            }
            default: {
                List<FluidFilter> filters = new ArrayList<>(tankCount);
                for (int i = 0; i < tankCount; i++) {
                    filters.add(inv().getFilterForTank(i));
                }
                return AggregateFluidFilter.anyOf(filters);
            }
        }
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return simpleDumbBadInsertionToBeRemoved(fluid, simulation);
    }

    private FluidVolume simpleDumbBadInsertionToBeRemoved(FluidVolume fluid, Simulation simulation) {
        fluid = fluid.copy();
        for (int t = 0; t < inv().getTankCount(); t++) {
            FluidVolume inTank = inv().getInvFluid(t);
            FluidAmount current = inTank.getAmount_F();
            FluidAmount max = current.roundedAdd(fluid.getAmount_F(), RoundingMode.DOWN).min(inv().getMaxAmount_F(t));
            FluidAmount addable = max.roundedSub(current, RoundingMode.DOWN);
            if (!addable.isPositive()) {
                continue;
            }
            inTank = inTank.copy();
            FluidVolume fluidCopy = fluid.copy();
            FluidVolume fluidAddable = fluidCopy.split(addable);
            FluidVolume merged = FluidVolume.merge(inTank, fluidAddable);

            if (merged != null && inv().setInvFluid(t, merged, simulation)) {
                fluid = fluidCopy;
                if (fluid.isEmpty()) {
                    return FluidVolumeUtil.EMPTY;
                }
            }
        }
        return fluid;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        if (maxAmount.isNegative()) {
            throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxAmount + ")");
        }
        FluidVolume fluid = FluidVolumeUtil.EMPTY;
        if (maxAmount.isZero()) {
            return fluid;
        }
        for (int t = 0; t < inv().getTankCount(); t++) {
            FluidAmount thisMax = maxAmount.roundedSub(fluid.getAmount_F(), RoundingMode.DOWN);
            fluid = FluidVolumeUtil.extractSingle(inv(), t, filter, fluid, thisMax, simulation);
            if (!fluid.getAmount_F().isLessThan(maxAmount)) {
                return fluid;
            }
        }
        return fluid;
    }
}
