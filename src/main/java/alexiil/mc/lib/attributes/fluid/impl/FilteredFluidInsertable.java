/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A {@link FluidInsertable} that delegates to another {@link FluidInsertable}, but has an additional filter as to what
 * can be inserted. */
public final class FilteredFluidInsertable implements FluidInsertable {

    private final FluidInsertable real;
    public final FluidFilter filter;

    public FilteredFluidInsertable(FluidInsertable real, FluidFilter filter) {
        this.real = real;
        this.filter = filter;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return real.getInsertionFilter().and(filter);
    }

    @Override
    public FluidAmount getMinimumAcceptedAmount() {
        return real.getMinimumAcceptedAmount();
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        if (!filter.matches(fluid.fluidKey)) {
            return fluid;
        }
        return real.attemptInsertion(fluid, simulation);
    }

    @Override
    public FluidInsertable filtered(FluidFilter filter) {
        return new FilteredFluidInsertable(real, filter.and(this.filter));
    }
}
