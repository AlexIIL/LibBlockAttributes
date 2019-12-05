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
import alexiil.mc.lib.attributes.fluid.FluidVolumeUtil;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInv;
import alexiil.mc.lib.attributes.fluid.GroupedFluidInvView;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** {@link GroupedFluidInvView} for an empty inventory. */
public enum EmptyGroupedFluidInv implements GroupedFluidInv {
    INSTANCE;

    @Override
    public FluidInvStatistic getStatistics(FluidFilter filter) {
        return FluidInvStatistic.emptyOf(filter);
    }

    @Override
    public Set<FluidKey> getStoredFluids() {
        return Collections.emptySet();
    }

    @Override
    public FluidAmount getTotalCapacity_F() {
        return FluidAmount.ZERO;
    }

    @Override
    public ListenerToken addListener_F(FluidInvAmountChangeListener_F listener, ListenerRemovalToken removalToken) {
        // We don't need to keep track of the listener because this empty inventory never changes.
        return () -> {
            // (And we don't need to do anything when the listener is removed)
        };
        // Never call the removal token as it's unnecessary (and saves the caller from re-adding it every tick)
    }

    @Override
    public FluidVolume attemptInsertion(FluidVolume fluid, Simulation simulation) {
        return fluid;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        return FluidVolumeUtil.EMPTY;
    }
}
