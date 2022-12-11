/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.filter;

import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import alexiil.mc.lib.attributes.fluid.volume.FluidEntry;
import alexiil.mc.lib.attributes.fluid.volume.FluidEntry.FluidFloatingEntry;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey;
import alexiil.mc.lib.attributes.fluid.volume.FluidRegistryEntry;

/** A {@link FluidFilter} that matches on what type it's based on. */
public abstract /* sealed */ class FluidTypeFilter implements ReadableFluidFilter {

    /* package-private */ FluidTypeFilter() {}

    /** Matches {@link FluidKey}s whose {@link FluidEntry} is a {@link FluidFloatingEntry}. */
    public static final FluidTypeFilter FLOATING = new FluidFloatingEntryFilter();

    /** Matches any {@link FluidKey} whose {@link FluidKey#getRawFluid()} returns a non-null {@link Fluid}. */
    public static final FluidTypeFilter RAW_FLUID = new RawFluidFilter();

    /** Matches any {@link FluidKey} whose {@link FluidEntry} is a {@link FluidRegistryEntry} and uses the
     * {@link Registries#POTION} registry. */
    public static final FluidTypeFilter POTION = get(Registries.POTION);

    /** @return A {@link FluidTypeFilter} that matches on the given registry. */
    public static final FluidTypeFilter get(Registry<?> registry) {
        return new RegisteredTypeFilter(registry);
    }

    public static final class FluidFloatingEntryFilter extends FluidTypeFilter {
        /* package-private */ FluidFloatingEntryFilter() {
            if (FLOATING != null) {
                throw new Error("Don't manually construct FluidFloatingEntryFilter!");
            }
        }

        @Override
        public boolean matches(FluidKey fluid) {
            return fluid.entry instanceof FluidFloatingEntry;
        }

        @Override
        public String toString() {
            return "any FluidFloatingEntry";
        }
    }

    public static final class RawFluidFilter extends FluidTypeFilter {
        /* package-private */ RawFluidFilter() {
            if (RAW_FLUID != null) {
                throw new Error("Don't manually construct RawFluidFilter!");
            }
        }

        @Override
        public boolean matches(FluidKey fluid) {
            return fluid.getRawFluid() != null;
        }

        @Override
        public String toString() {
            return "FluidKey.getRawFluid()!=null";
        }
    }

    public static final class RegisteredTypeFilter extends FluidTypeFilter {

        public final Registry<?> registry;

        /* package-private */ RegisteredTypeFilter(Registry<?> registry) {
            this.registry = registry;
        }

        @Override
        public boolean matches(FluidKey fluid) {
            FluidEntry entry = fluid.entry;
            if (entry instanceof FluidRegistryEntry) {
                return ((FluidRegistryEntry) entry).getBackingRegistry() == registry;
            }
            return false;
        }

        @Override
        public String toString() {
            return "FluidRegistryEntry of " + FluidRegistryEntry.getFullRegistryName(registry);
        }
    }
}
