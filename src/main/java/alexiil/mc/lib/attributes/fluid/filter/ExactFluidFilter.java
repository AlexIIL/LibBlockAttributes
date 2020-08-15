/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import net.minecraft.fluid.Fluid;
import net.minecraft.potion.Potion;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;

/** An {@link FluidFilter} that only matches on a single {@link FluidKey}. */
public final class ExactFluidFilter implements ReadableFluidFilter {

    public final FluidKey fluid;

    public ExactFluidFilter(FluidKey fluid) {
        this.fluid = fluid;
    }

    @Override
    public boolean matches(FluidKey other) {
        return fluid.equals(other);
    }

    public static ReadableFluidFilter of(Fluid fluid) {
        return new ExactFluidFilter(FluidKeys.get(fluid));
    }

    public static ReadableFluidFilter of(Potion potion) {
        return new ExactFluidFilter(FluidKeys.get(potion));
    }
}
