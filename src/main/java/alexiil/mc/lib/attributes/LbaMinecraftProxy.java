/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import net.minecraft.fluid.Fluid;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;

/** Tiny abstraction API over changes in the few parts of minecraft that LBA actually uses. Mods are <em>not</em>
 * expected to use this. */
public abstract class LbaMinecraftProxy {

    // Abstract methods

    public abstract Biome getBiome(WorldView view, BlockPos pos);
    public abstract boolean isInTag(Fluid fluid, Tag<Fluid> tag);

    // Internals

    private static LbaMinecraftProxy proxy;

    public static LbaMinecraftProxy instance() {
        if (proxy == null) {
            proxy = createProxy();
        }
        return proxy;
    }

    private static LbaMinecraftProxy createProxy() {
        ModContainer mc = FabricLoader.getInstance().getModContainer("minecraft").orElseThrow();
        Version mcVersion = mc.getMetadata().getVersion();

        try {
            if (mcVersion.compareTo(Version.parse("1.18.2")) < 0) {
                return new _1_18();
            } else {
                return new _1_18_2();
            }
        } catch (VersionParsingException e) {
            throw new IllegalStateException("Hardcoded version string failed to parse as a version!", e);
        } catch (ReflectiveOperationException e) {
            throw new Error("Something went wrong while creating the proxy, things are about to go very wrong...", e);
        }
    }

    private static MethodHandle findVirtual(Lookup lookup, Class<?> in, MethodType type, String... names)
        throws ReflectiveOperationException {
        for (String name : names) {
            try {
                return lookup.findVirtual(in, name, type);
            } catch (NoSuchMethodException e) {
                continue;
            }
        }

        throw new NoSuchMethodException(
            "Unable to find " + in + "." + type + " called any of " + Arrays.toString(names)
        );
    }

    /** Versions before 1.18.2. As we don't compile against this version we have to use reflection instead. */
    static final class _1_18 extends LbaMinecraftProxy {

        private final MethodHandle worldView_getBiome;
        private final MethodHandle fluid_isIn_Tag;

        public _1_18() throws ReflectiveOperationException {
            Lookup lookup = MethodHandles.lookup();
            worldView_getBiome = findVirtual(
                lookup, WorldView.class, MethodType.methodType(Biome.class, BlockPos.class), "getBiome", "method_23753"
            );
            fluid_isIn_Tag = findVirtual(
                lookup, Fluid.class, MethodType.methodType(boolean.class, Tag.class), "isIn", "method_15791"
            );
        }

        @Override
        public Biome getBiome(WorldView view, BlockPos pos) {
            try {
                return (Biome) worldView_getBiome.invokeExact(view, pos);
            } catch (RuntimeException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                throw new Error(e);
            }
        }

        @Override
        public boolean isInTag(Fluid fluid, Tag<Fluid> tag) {
            try {
                return (boolean) fluid_isIn_Tag.invokeExact(fluid, tag);
            } catch (RuntimeException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Throwable e) {
                throw new Error(e);
            }
        }
    }

    /** The version of minecraft that this version of LBA is compiled against. As such this doesn't use any
     * reflection. */
    static final class _1_18_2 extends LbaMinecraftProxy {

        @Override
        public Biome getBiome(WorldView view, BlockPos pos) {
            return view.getBiome(pos).value();
        }

        @Override
        public boolean isInTag(Fluid fluid, Tag<Fluid> tag) {
            return tag.values().contains(fluid);
        }
    }
}
