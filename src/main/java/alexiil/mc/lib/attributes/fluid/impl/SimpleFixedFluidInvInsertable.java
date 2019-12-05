/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidInsertable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** @deprecated This has been replaced with {@link FixedFluidInv#getMappedInv(int...)} followed by
 *             {@link FixedFluidInv#getInsertable()}. (And optionally {@link FluidInsertable#getPureInsertable()} if you
 *             only want to expose it as an insertable). */
@Deprecated
public final class SimpleFixedFluidInvInsertable implements FluidInsertable {

    private final FluidInsertable real;

    public SimpleFixedFluidInvInsertable(FixedFluidInv inv, int[] tanks) {
        this.real = (tanks == null ? inv : inv.getMappedInv(tanks)).getInsertable();
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return real.attemptInsertion(fluid, simulation);
    }

    @Override
    public FluidAmount getMinimumAcceptedAmount() {
        return real.getMinimumAcceptedAmount();
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return real.getInsertionFilter();
    }
}
