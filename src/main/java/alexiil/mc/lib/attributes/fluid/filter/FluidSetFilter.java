/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import java.util.Collections;
import java.util.Set;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A {@link FluidFilter} that matches any {@link FluidKey} in a {@link Set} of {@link FluidKey}s. */
public final class FluidSetFilter implements ReadableFluidFilter {

    private final Set<FluidKey> fluids;

    public FluidSetFilter(Set<FluidKey> fluids) {
        this.fluids = Collections.unmodifiableSet(fluids);
    }

    @Override
    public boolean matches(FluidKey fluidKey) {
        return fluids.contains(fluidKey);
    }

    /** @return The set of fluids that this matches. */
    public Set<FluidKey> getFluids() {
        return fluids;
    }
}
