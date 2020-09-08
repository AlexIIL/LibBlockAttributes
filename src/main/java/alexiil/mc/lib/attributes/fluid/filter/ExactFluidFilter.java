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

    /** Creates a new {@link ExactFluidFilter}. There's generally little point in using this, as every {@link FluidKey}
     * contains a field for storing this ({@link FluidKey#exactFilter}). */
    public ExactFluidFilter(FluidKey fluid) {
        this.fluid = fluid;
    }

    @Override
    public boolean matches(FluidKey other) {
        return fluid.equals(other);
    }

    public static ReadableFluidFilter of(Fluid fluid) {
        return FluidKeys.get(fluid).exactFilter;
    }

    public static ReadableFluidFilter of(Potion potion) {
        return FluidKeys.get(potion).exactFilter;
    }

    public static ReadableFluidFilter of(FluidKey fluid) {
        return fluid.exactFilter;
    }
}
