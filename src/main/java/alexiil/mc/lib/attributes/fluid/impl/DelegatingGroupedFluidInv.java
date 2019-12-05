/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.impl;

import java.util.Collections;
import java.util.Set;

import alexiil.mc.lib.attributes.ListenerRemovalToken;
import alexiil.mc.lib.attributes.ListenerToken;
import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.fluid.FluidInvAmountChangeListener_F;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

public class DelegatingGroupedFluidInv implements GroupedFluidInv {

    protected final GroupedFluidInv delegate;

    public DelegatingGroupedFluidInv(GroupedFluidInv delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        return Collections.unmodifiableSet(delegate.getStoredFluids());
    }

    @Override
    public FluidAmount getTotalCapacity_F() {
        return delegate.getTotalCapacity_F();
    }

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        return delegate.getStatistics(filter);
    }

    @Override
    public ListenerToken addListener_F(FluidInvAmountChangeListener_F listener, ListenerRemovalToken removalToken) {
        GroupedFluidInv real = this;
        return delegate.addListener_F((inv, fluid, previous, current) -> {
            listener.onChange(real, fluid, previous, current);
        }, removalToken);
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return delegate.attemptInsertion(fluid, simulation);
    }

    @Override
    public FluidFilter getInsertionFilter() {
        return delegate.getInsertionFilter();
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return delegate.attemptExtraction(filter, maxAmount, simulation);
    }

    @Override
    public FluidVolume attemptAnyExtraction(FluidAmount maxAmount, Simulation simulation) {
        return delegate.attemptAnyExtraction(maxAmount, simulation);
    }
}
