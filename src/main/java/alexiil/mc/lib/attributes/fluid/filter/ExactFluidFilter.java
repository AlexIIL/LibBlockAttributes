/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

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
}
