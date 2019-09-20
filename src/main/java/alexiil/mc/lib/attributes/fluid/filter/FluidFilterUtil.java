/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import alexiil.mc.lib.attributes.AggregateFilterType;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

public final class FluidFilterUtil {
    private FluidFilterUtil() {}

    public static boolean hasIntersection(FluidFilter a, FluidFilter b) {

        FluidFilter combined = a.and(b);
        return combined != ConstantFluidFilter.NOTHING;
    }

    /** Attempts to decompose the given filter down into it's component {@link FluidKey}'s, if it implements
     * {@link ReadableFluidFilter}.
     * 
     * @return Null if the set could not be computed (either because the filter was {@link ConstantFluidFilter#ANYTHING}
     *         or if it was not readable in some other way). */
    @Nullable
    public static Set<FluidKey> decomposeFilter(FluidFilter filter) {
        if (!(filter instanceof ReadableFluidFilter)) {
            return null;
        } else if (filter == ConstantFluidFilter.NOTHING) {
            return Collections.emptySet();
        } else if (filter == ConstantFluidFilter.ANYTHING) {
            return null;
        } else if (filter instanceof ExactFluidFilter) {
            return Collections.singleton(((ExactFluidFilter) filter).fluid);
        } else if (filter instanceof AggregateFluidFilter) {
            AggregateFluidFilter aggregate = (AggregateFluidFilter) filter;

            if (aggregate.type == AggregateFilterType.ALL) {

                Set<FluidKey> soFar = null;
                for (int i = 0; i < aggregate.getFilterCount(); i++) {
                    Set<FluidKey> decomposed = decomposeFilter(aggregate.getFilter(i));
                    if (decomposed == null) {
                        return null;
                    }
                    if (soFar == null) {
                        soFar = decomposed;
                    } else {
                        soFar.retainAll(decomposed);
                    }
                }
                return soFar;

            } else {

                Set<FluidKey> matches = new HashSet<>();
                for (int i = 0; i < aggregate.getFilterCount(); i++) {
                    Set<FluidKey> decomposed = decomposeFilter(aggregate.getFilter(i));
                    if (decomposed == null) {
                        return null;
                    }
                    matches.addAll(decomposed);
                }
                return matches;
            }
        } else {
            ReadableFluidFilter.checkValidity((ReadableFluidFilter) filter);
            return null;
        }
    }
}
