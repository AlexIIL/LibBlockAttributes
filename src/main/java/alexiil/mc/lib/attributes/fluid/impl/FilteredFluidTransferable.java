/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidTransferable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class FilteredFluidTransferable implements FluidTransferable {
    private final FluidTransferable real;
    public final FluidFilter filter;

    public FilteredFluidTransferable(FluidTransferable real, FluidFilter filter) {
        this.real = real;
        this.filter = filter;
    }

    // FluidExtractable

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        FluidFilter realFilter = this.filter.and(filter);
        return real.attemptExtraction(realFilter, maxAmount, simulation);
    }

    // FluidInsertable

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

    // FluidTransferable

    @Override
    public FluidTransferable filtered(FluidFilter filter) {
        return new FilteredFluidTransferable(real, filter.and(this.filter));
    }
}
