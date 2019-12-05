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
import alexiil.mc.lib.attributes.fluid.FluidExtractable;
import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** @deprecated This has been replaced with {@link FixedFluidInv#getMappedInv(int...)} followed by
 *             {@link FixedFluidInv#getExtractable()}. (And optionally {@link FluidExtractable#getPureExtractable()} if
 *             you only want to expose it as an extractable). */
@Deprecated
public final class SimpleFixedFluidInvExtractable implements FluidExtractable {

    private final FluidExtractable real;

    public SimpleFixedFluidInvExtractable(FixedFluidInv inv, int[] tanks) {
        this.real = (tanks == null ? inv : inv.getMappedInv(tanks)).getExtractable();
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, FluidAmount maxAmount, Simulation simulation) {
        return real.attemptExtraction(filter, maxAmount, simulation);
    }
}
