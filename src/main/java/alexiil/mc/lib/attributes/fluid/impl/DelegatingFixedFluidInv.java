/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FixedFluidInv;
import alexiil.mc.lib.attributes.fluid.FluidInvTankChangeListener;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** A simple delegate base class for {@link FixedFluidInv}. */
public class DelegatingFixedFluidInv implements FixedFluidInv {

    protected final FixedFluidInv delegate;

    public DelegatingFixedFluidInv(FixedFluidInv delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getTankCount() {
        return delegate.getTankCount();
    }

    @Override
    public FluidVolume getInvFluid(int tank) {
        return delegate.getInvFluid(tank);
    }

    @Override
    @Deprecated(since = "0.6.0", forRemoval = true)
    public int getMaxAmount(int tank) {
        return delegate.getMaxAmount(tank);
    }

    @Override
    public FluidAmount getMaxAmount_F(int tank) {
        return delegate.getMaxAmount_F(tank);
    }

    @Override
    public boolean isFluidValidForTank(int tank, FluidKey fluid) {
        return delegate.isFluidValidForTank(tank, fluid);
    }

    @Override
    public FluidFilter getFilterForTank(int tank) {
        return delegate.getFilterForTank(tank);
    }

    @Override
    public ListenerToken addListener(FluidInvTankChangeListener listener, ListenerRemovalToken removalToken) {
        FixedFluidInv wrapper = this;
        return delegate.addListener((realInv, tank, previous, current) -> {
            listener.onChange(wrapper, tank, previous, current);
        }, removalToken);
    }

    @Override
    public boolean setInvFluid(int tank, FluidVolume to, Simulation simulation) {
        return delegate.setInvFluid(tank, to, simulation);
    }
}
