/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public final class FilteredFluidExtractable implements FluidExtractable {
    private final FluidExtractable real;
    public final FluidFilter filter;

    public FilteredFluidExtractable(FluidExtractable real, FluidFilter filter) {
        this.real = real;
        this.filter = filter;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        FluidFilter realFilter = this.filter.and(filter);
        return real.attemptExtraction(realFilter, maxAmount, simulation);
    }

    @Override
    public FluidExtractable filtered(FluidFilter filter) {
        return new FilteredFluidExtractable(real, this.filter.and(filter));
    }
}
