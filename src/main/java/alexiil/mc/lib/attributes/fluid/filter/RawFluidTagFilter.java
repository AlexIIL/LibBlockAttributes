/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import java.util.HashSet;

import net.minecraft.fluid.Fluid;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;

import alexiil.mc.lib.attributes.LbaMinecraftProxy;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;

/** A {@link FluidFilter} that operates on a {@link Tag} of vanilla minecraft's {@link Fluid}'s. */
public final class RawFluidTagFilter implements ResolvableFluidFilter {

    @Deprecated
    public final Tag<Fluid> tag;

    private TagKey<Fluid> tagKey;

    /** 1.18.1 constructor. Don't use this for 1.18.2 or later. */
    @Deprecated
    public RawFluidTagFilter(Tag<Fluid> tag) {
        this.tag = tag;
    }

    /** 1.18.2 constructor. Don't use this for 1.18.1 or earlier. */
    public RawFluidTagFilter(TagKey<Fluid> tagKey) {
        this.tagKey = tagKey;
        this.tag = null;
    }

    @Override
    public boolean matches(FluidKey fluidKey) {
        Fluid raw = fluidKey.getRawFluid();
        if (raw == null) {
            return false;
        }
        if (tag != null) {
            return LbaMinecraftProxy.instance().isInTag(raw, tag);
        }
        return raw.isIn(tagKey);
    }

    @Override
    public ReadableFluidFilter resolve() {
        HashSet<FluidKey> set = new HashSet<>();
        for (Fluid fluid : tag.values()) {
            set.add(FluidKeys.get(fluid));
        }
        return new FluidSetFilter(set);
    }
}
