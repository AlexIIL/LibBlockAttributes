/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A {@link FluidFilter} that checks to see if the given {@link FluidInsertable} could have the fluid inserted into it,
 * right now. (Note that this doesn't match the definition of {@link FluidInsertable#getInsertionFilter()}, so you
 * should never use it a return value from that). */
public final class FluidInsertableFilter implements FluidFilter {

    public final FluidInsertable insertable;

    public FluidInsertableFilter(FluidInsertable insertable) {
        this.insertable = insertable;
    }

    @Override
    public boolean matches(FluidKey fluidKey) {
        FluidVolume volume = fluidKey.withAmount(FluidAmount.A_MILLION);
        return insertable.attemptInsertion(volume, Simulation.SIMULATE).getAmount_F().isLessThan(FluidAmount.A_MILLION);
    }
}
