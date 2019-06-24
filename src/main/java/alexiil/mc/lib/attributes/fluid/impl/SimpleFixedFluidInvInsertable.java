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
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** @deprecated Use {@link GroupedFluidInvFixedWrapper} instead of this! */
@Deprecated
public final class SimpleFixedFluidInvInsertable implements FluidInsertable {

    private final FixedFluidInv inv;

    /** Null means that this can insert into any of the tanks. */
    private final int[] tanks;

    public SimpleFixedFluidInvInsertable(FixedFluidInv inv, int[] tanks) {
        this.inv = inv;
        this.tanks = tanks;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        if (tanks == null) {
            int tankCount = inv.getTankCount();
            switch (tankCount) {
                case 0: {
                    // What?
                    return ConstantFluidFilter.NOTHING;
                }
                case 1: {
                    return inv.getFilterForTank(0);
                }
                case 2: {
                    return inv.getFilterForTank(0).and(inv.getFilterForTank(1));
                }
                default: {
                    List<FluidFilter> filters = new ArrayList<>(tankCount);
                    for (int i = 0; i < tankCount; i++) {
                        filters.add(inv.getFilterForTank(i));
                    }
                    return AggregateFluidFilter.anyOf(filters);
                }
            }
        } else {
            switch (tanks.length) {
                case 0: {
                    // What?
                    return ConstantFluidFilter.NOTHING;
                }
                case 1: {
                    return inv.getFilterForTank(tanks[0]);
                }
                case 2: {
                    return inv.getFilterForTank(tanks[0]).and(inv.getFilterForTank(tanks[1]));
                }
                default: {
                    List<FluidFilter> filters = new ArrayList<>(tanks.length);
                    for (int s : tanks) {
                        filters.add(inv.getFilterForTank(s));
                    }
                    return AggregateFluidFilter.anyOf(filters);
                }
            }
        }
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        // FluidVolume leftover = stack.copy();
        //
        // // First: scan the available tanks to see if we can add to an existing stack
        //
        // IntList tanksModified = new IntArrayList();
        //
        // if (tanks == null) {
        // for (int s = 0; s < inv.getInvSize(); s++) {
        // attemptAddToExisting(tanksModified, s, leftover, simulation);
        // }
        // } else {
        // for (int s : tanks) {
        // attemptAddToExisting(tanksModified, s, leftover, simulation);
        // }
        // }

        return simpleDumbBadInsertionToBeRemoved(stack, simulation);
    }

    private FluidVolume simpleDumbBadInsertionToBeRemoved(FluidVolume fluid, Simulation simulation) {
        fluid = fluid.copy();
        if (tanks == null) {
            for (int t = 0; t < inv.getTankCount(); t++) {
                FluidVolume inTank = inv.getInvFluid(t);
                int current = inTank.getAmount();
                int max = Math.min(current + fluid.getAmount(), inv.getMaxAmount(t));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                inTank = inTank.copy();
                FluidVolume fluidCopy = fluid.copy();
                FluidVolume fluidAddable = fluidCopy.split(addable);
                FluidVolume merged = FluidVolume.merge(inTank, fluidAddable);

                if (merged != null && inv.setInvFluid(t, merged, simulation)) {
                    fluid = fluidCopy;
                    if (fluid.isEmpty()) {
                        return FluidKeys.EMPTY.withAmount(0);
                    }
                }
            }
        } else {
            for (int t : tanks) {
                // Copy of above
                FluidVolume inTank = inv.getInvFluid(t);
                int current = inTank.isEmpty() ? 0 : inTank.getAmount();
                int max = Math.min(current + fluid.getAmount(), inv.getMaxAmount(t));
                int addable = max - current;
                if (addable <= 0) {
                    continue;
                }
                FluidVolume fluidCopy = fluid.copy();
                FluidVolume fluidAddable = fluidCopy.split(addable);
                FluidVolume merged = FluidVolume.merge(inTank, fluidAddable);

                if (merged != null && inv.setInvFluid(t, merged, simulation)) {
                    fluid = fluidCopy;
                    if (fluid.isEmpty()) {
                        return FluidKeys.EMPTY.withAmount(0);
                    }
                }
            }
        }
        return fluid;
    }
}
