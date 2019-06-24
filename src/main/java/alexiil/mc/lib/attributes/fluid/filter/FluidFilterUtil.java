/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

public enum FluidFilterUtil {
    ;

    public static boolean hasIntersection(FluidFilter a, FluidFilter b) {
        FluidFilter combined = a.and(b);
        return combined != ConstantFluidFilter.NOTHING;
    }
}
