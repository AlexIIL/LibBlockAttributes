/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A type of {@link ReadableFluidFilter} that may be resolved at a later time to a fixed {@link ReadableFluidFilter},
 * but which isn't constant over time. (For example item tags may be reloaded, but are stored in a static field and so
 * don't need to be re-created often. (And can be read)). */
@FunctionalInterface
public interface ResolvableFluidFilter extends ReadableFluidFilter {

    /** Resolves this {@link ReadableFluidFilter}. Note that this must return one of the valid types of
     * {@link ReadableFluidFilter}, except this!
     * <p>
     * This is intended for use-cases where you actually need to read the filter - for other cases there's usually
     * little reason to call this, as "matches" should be faster for most reasonable implementations. (However some
     * implementations - for example lambdas - might be faster to call multiple times if you resolve them before
     * checking them). */
    ReadableFluidFilter resolve();

    /** {@inheritDoc}
     * <p>
     * This is overridden primarily for lambdas - most implementing classes are expected to override this, because it
     * will most likely be faster. */
    @Override
    default boolean matches(FluidKey fluid) {
        ReadableFluidFilter resolved = resolve();
        if (resolved instanceof ResolvableFluidFilter) {
            throw new IllegalStateException(
                getClass() + "'s 'resolve()' method returned " + resolved + ", which isn't a fixed filter!"
            );
        }
        ReadableFluidFilter.checkValidity(resolved);
        return resolved.matches(fluid);
    }
}
