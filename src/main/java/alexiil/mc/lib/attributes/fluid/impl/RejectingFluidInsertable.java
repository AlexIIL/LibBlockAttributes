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
import alexiil.mc.lib.attributes.fluid.filter.ConstantFluidFilter;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;
import alexiil.mc.lib.attributes.misc.NullVariant;

/** An {@link FluidInsertable} that always refuses to accept any inserted {@link FluidVolume}. */
public enum RejectingFluidInsertable implements FluidInsertable, NullVariant {
    NULL,
    EXTRACTOR;

    @Override
    public FluidVolume attemptInsertion(FluidVolume stack, Simulation simulation) {
        return stack;
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return ConstantFluidFilter.NOTHING;
    }

    @Override
    public FluidInsertable getPureInsertable() {
        return this;
    }
}
