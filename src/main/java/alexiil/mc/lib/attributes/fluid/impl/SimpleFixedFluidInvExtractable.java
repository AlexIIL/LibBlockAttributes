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
import alexiil.mc.lib.attributes.fluid.filter.FluidFilter;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;
import alexiil.mc.lib.attributes.fluid.volume.FluidVolume;

/** @deprecated Use {@link GroupedFluidInvFixedWrapper} instead of this! */
@Deprecated
public final class SimpleFixedFluidInvExtractable implements FluidExtractable {

    private final FixedFluidInv inv;

    /** Null means that this can extract from any of the tanks. */
    private final int[] tanks;

    public SimpleFixedFluidInvExtractable(FixedFluidInv inv, int[] tanks) {
        this.inv = inv;
        this.tanks = tanks;
    }

    @Override
    public FluidVolume attemptExtraction(FluidFilter filter, int maxAmount, Simulation simulation) {
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxAmount cannot be negative! (was " + maxAmount + ")");
        }
        FluidVolume fluid = FluidKeys.EMPTY.withAmount(0);
        if (maxAmount == 0) {
            return fluid;
        }
        if (tanks == null) {
            for (int t = 0; t < inv.getTankCount(); t++) {
                FluidVolume invFluid = inv.getInvFluid(t);
                if (invFluid.isEmpty() || !filter.matches(invFluid.fluidKey)) {
                    continue;
                }
                invFluid = invFluid.copy();
                FluidVolume addable = invFluid.split(maxAmount);
                FluidVolume merged = FluidVolume.merge(fluid, addable);
                if (merged != null && inv.setInvFluid(t, invFluid, simulation)) {
                    maxAmount -= addable.getAmount();
                    fluid = merged;
                    assert maxAmount >= 0;
                    if (maxAmount <= 0) {
                        return fluid;
                    }
                }
            }
        } else {
            for (int t : tanks) {
                // Copy of above
                FluidVolume invFluid = inv.getInvFluid(t);
                if (invFluid.isEmpty() || !filter.matches(invFluid.fluidKey)) {
                    continue;
                }
                invFluid = invFluid.copy();
                FluidVolume addable = invFluid.split(maxAmount);
                FluidVolume merged = FluidVolume.merge(fluid, addable);
                if (merged != null && inv.setInvFluid(t, invFluid, simulation)) {
                    maxAmount -= addable.getAmount();
                    fluid = merged;
                    assert maxAmount >= 0;
                    if (maxAmount <= 0) {
                        return fluid;
                    }
                }
            }
        }

        return fluid;
    }
}
