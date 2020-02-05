/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A {@link FluidFilter} that either matches everything ({@link #ANYTHING}) or matches nothing ({@link #NOTHING}) */
public enum ConstantFluidFilter implements ReadableFluidFilter {
    /** A {@link FluidFilter} that matches everything. */
    ANYTHING(true),
    /** A {@link FluidFilter} that matches nothing. */
    NOTHING(false);

    private final boolean result;

    private ConstantFluidFilter(boolean result) {
        this.result = result;
    }

    public static ConstantFluidFilter of(boolean result) {
        return result ? ANYTHING : NOTHING;
    }

    @Override
    public boolean matches(FluidKey fluid) {
        if (fluid.isEmpty()) {
            throw new IllegalArgumentException("You should never test a FluidFilter with an empty fluid!");
        }
        return result;
    }

    @Override
    public FluidFilter negate() {
        return of(!result);
    }

    @Override
    public FluidFilter and(FluidFilter other) {
        if (result) {
            return other;
        } else {
            return NOTHING;
        }
    }

    @Override
    public FluidFilter or(FluidFilter other) {
        if (result) {
            return ANYTHING;
        } else {
            return other;
        }
    }

    // Don't override asPredicate so that we still get the better version that calls our own negate(), and(), or()
    // methods.
}
