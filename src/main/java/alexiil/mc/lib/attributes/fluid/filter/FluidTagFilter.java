/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A {@link FluidFilter} that operates on tags of LBA's {@link FluidKey}'s.
 * <p>
 * Note that this <strong>has not been implemented yet!</strong> Instead you'll have to use the
 * {@link RawFluidTagFilter} to use tags (but only for raw vanilla minecraft fluids) */
public final class FluidTagFilter implements ResolvableFluidFilter {
    private FluidTagFilter() {}

    @Override
    public boolean matches(FluidKey fluidKey) {
        return false;
    }

    @Override
    public ReadableFluidFilter resolve() {
        return ConstantFluidFilter.NOTHING;
    }
}
