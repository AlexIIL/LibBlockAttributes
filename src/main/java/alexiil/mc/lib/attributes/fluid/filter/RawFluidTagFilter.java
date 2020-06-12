/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import net.minecraft.fluid.Fluid;
import net.minecraft.tag.Tag;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;

/** A {@link FluidFilter} that operates on a {@link Tag} of vanilla minecraft's {@link Fluid}'s. */
public final class RawFluidTagFilter implements ReadableFluidFilter {

    public final Tag<Fluid> tag;

    public RawFluidTagFilter(Tag<Fluid> tag) {
        this.tag = tag;
    }

    @Override
    public boolean matches(FluidKey fluidKey) {
        Fluid raw = fluidKey.getRawFluid();
        if (raw == null) {
            return false;
        }
        return raw.isIn(tag);
    }
}
