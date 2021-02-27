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
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.AggregateFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.AbstractCombined;

public final class CombinedFluidInsertable extends AbstractCombined<FluidInsertable> implements FluidInsertable {

    public CombinedFluidInsertable(List<? extends FluidInsertable> list) {
        super(list);
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        for (FluidInsertable insertable : list) {
            stack = insertable.attemptInsertion(stack, simulation);
            if (stack.isEmpty()) {
                return FluidVolumeUtil.EMPTY;
            }
        }
        return stack;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        List<FluidFilter> filters = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            filters.add(list.get(i).getInsertionFilter());
        }
        return AggregateFluidFilter.anyOf(filters);
    }

    @Override
    public FluidAmount getMinimumAcceptedAmount() {
        FluidAmount fa = null;
        for (FluidInsertable fi : list) {
            FluidAmount fa2 = fi.getMinimumAcceptedAmount();
            if (fa2 == null) {
                return null;
            }
            if (fa == null) {
                fa = fa2;
            } else {
                fa = fa.min(fa2);
            }
        }
        return fa;
    }
}
