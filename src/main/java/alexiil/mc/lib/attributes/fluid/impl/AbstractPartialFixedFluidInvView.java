/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** Base class for {@link SubFixedFluidInvView} and {@link MappedFixedFluidInvView}. */
public abstract class AbstractPartialFixedFluidInvView implements FixedFluidInvView {

    /** The inventory that is wrapped. */
    protected final FixedFluidInvView inv;

    protected AbstractPartialFixedFluidInvView(FixedFluidInvView inv) {
        this.inv = inv;
    }

    /** @return The tank that the internal {@link #inv} should use. */
    protected abstract int getInternalTank(int tank);

    @Override
    public FluidVolume getInvFluid(int tank) {
        return inv.getInvFluid(getInternalTank(tank));
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return inv.isFluidValidForTank(getInternalTank(tank), fluid);
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        return inv.getFilterForTank(getInternalTank(tank));
    }

    @Override
    @Deprecated(since = "0.6.0", forRemoval = true)
    public int getMaxAmount(int tank) {
        return inv.getMaxAmount(getInternalTank(tank));
    }

    @Override
    public FluidAmount getMaxAmount_F(int tank) {
        return inv.getMaxAmount_F(getInternalTank(tank));
    }
}
