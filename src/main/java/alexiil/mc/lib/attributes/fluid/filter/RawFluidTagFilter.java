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
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;

import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidKeys;

/** A {@link FluidFilter} that operates on a {@link TagKey} of vanilla minecraft's {@link Fluid}'s. */
public final class RawFluidTagFilter implements ResolvableFluidFilter {

    private TagKey<Fluid> tagKey;

    public RawFluidTagFilter(TagKey<Fluid> tagKey) {
        this.tagKey = tagKey;
    }

    @Override
    public boolean matches(FluidKey fluidKey) {
        Fluid raw = fluidKey.getRawFluid();
        if (raw == null) {
            return false;
        }
        return raw.isIn(tagKey);
    }

    @Override
    public ReadableFluidFilter resolve() {
        HashSet<FluidKey> set = new HashSet<>();
        for (RegistryEntry<Fluid> entry : Registries.FLUID.iterateEntries(tagKey)) {
            set.add(FluidKeys.get(entry.value()));
        }
        return new FluidSetFilter(set);
    }
}
