/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import alexiil.mc.lib.attributes.fluid.amount.FluidAmount;
import alexiil.mc.lib.attributes.fluid.volume.FluidEntry.FluidFloatingEntry;
import alexiil.mc.lib.attributes.fluid.volume.FluidKey.FluidKeyBuilder;

/** The central registry for storing {@link FluidKey} instances, and mapping {@link Fluid} and {@link Potion} instances
 * to them. */
public final class FluidKeys {
    private FluidKeys() {}

    public static final Identifier MISSING_SPRITE = new Identifier("minecraft", "missingno");

    public static final FluidKey EMPTY;
    public static final FluidKey LAVA;
    public static final BiomeSourcedFluidKey WATER;

    private static final Map<Fluid, FluidKey> FLUIDS = new IdentityHashMap<>();
    private static final Map<Potion, FluidKey> POTIONS = new IdentityHashMap<>();
    private static final Map<FluidRegistryEntry<?>, FluidKey> OTHERS = new HashMap<>();
    private static final Map<FluidFloatingEntry, FluidKey> FLOATING = new HashMap<>();

    /*
     * Synchronisation notes:
     *
     * In theory this should use a ReadWriteLock to allow all of the
     * readers to read without blocking each other. However in practice
     * there's only going to be 2 threads accessing this (client and
     * server) so there's probably no point.
     *
     * (In addition the only operations that is performed is HashMap.get
     *  anyway, so we would probably only loose performance).  
     */

    static {
        // Empty doesn't have a proper sprite or text component because it doesn't ever make sense to use it.
        EMPTY = new SimpleFluidKey(
            new FluidKeyBuilder(Fluids.EMPTY)//
                .setName(Text.translatable("libblockattributes.fluid.empty"))
        );

        LAVA = new SimpleFluidKey(
            new FluidKeyBuilder(Fluids.LAVA)//
                .setName(Text.translatable(Blocks.LAVA.getTranslationKey())//
                    .setStyle(Style.EMPTY.withColor(Formatting.RED))
                )//
                .setLuminosity(15)//
                .setViscosity(FluidAmount.of(30, 5))//
                .setNetherViscosity(FluidAmount.of(10, 5))//
                .setCohesion(FluidAmount.ofWhole(2))//
                .setNetherCohesion(FluidAmount.ofWhole(4))//
        );

        WATER = WaterFluidKey.INSTANCE;
        ((MutableText) WATER.name).setStyle(Style.EMPTY.withColor(Formatting.BLUE));

        put(Fluids.EMPTY, EMPTY);
        put(Fluids.LAVA, LAVA);
        put(Fluids.WATER, WATER);
        put(Potions.EMPTY, EMPTY);
        put(Potions.WATER, WATER);
    }

    public static synchronized void put(Fluid fluid, FluidKey fluidKey) {
        FLUIDS.put(fluid, fluidKey);
        if (fluid instanceof FlowableFluid) {
            FLUIDS.put(((FlowableFluid) fluid).getStill(), fluidKey);
            FLUIDS.put(((FlowableFluid) fluid).getFlowing(), fluidKey);
        }
    }

    public static synchronized void put(Potion potion, FluidKey fluidKey) {
        POTIONS.put(potion, fluidKey);
    }

    public static synchronized void put(FluidRegistryEntry<?> entry, FluidKey fluidKey) {
        if (entry.backingRegistry == Registries.FLUID) {
            put((Fluid) entry.backingObject, fluidKey);
        } else if (entry.backingObject == Registries.POTION) {
            put((Potion) entry.backingObject, fluidKey);
        } else {
            OTHERS.put(entry, fluidKey);
        }
    }

    public static synchronized void put(FluidFloatingEntry entry, FluidKey fluidKey) {
        FLOATING.put(entry, fluidKey);
    }

    /** Removes a fluid entry from this map.
     * 
     * @deprecated Because I think fluids are meant to be all statically created? */
    @Deprecated(forRemoval = false)
    public static synchronized void remove(Fluid fluid) {
        FLUIDS.remove(fluid);
    }

    /** @return Null if the passed fluid is null, or a non-null {@link FluidKey}. */
    public static synchronized FluidKey get(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        FluidKey fluidKey = FLUIDS.get(fluid);
        if (fluidKey == null) {
            if (fluid instanceof FlowableFluid) {
                FlowableFluid base = (FlowableFluid) fluid;
                fluid = base.getStill();
                if (fluid == null) {
                    throw new IllegalStateException("fluid.getStill() returned a null fluid! (from " + fluid + ")");
                }
            }
            fluidKey = createImplicitFluid(fluid);
            put(fluid, fluidKey);
        }
        return fluidKey;
    }

    private static SimpleFluidKey createImplicitFluid(Fluid fluid) {
        Block block = fluid.getDefaultState().getBlockState().getBlock();
        Text name = Text.translatable(block.getTranslationKey());
        FluidKeyBuilder builder = new FluidKeyBuilder(fluid).setName(name);
        if (fluid instanceof FluidKeyCustomiser) {
            ((FluidKeyCustomiser) fluid).customiseKey(builder);
        }
        return new SimpleFluidKey(builder);
    }

    public static synchronized FluidKey get(Potion potion) {
        FluidKey fluidKey = POTIONS.get(potion);
        if (fluidKey == null) {
            fluidKey = new PotionFluidKey(potion);
            put(potion, fluidKey);
        }
        return fluidKey;
    }

    @Nullable
    public static synchronized FluidKey get(FluidEntry entry) {
        if (entry instanceof FluidFloatingEntry) {
            return FLOATING.get(entry);
        }
        if (entry instanceof FluidRegistryEntry<?>) {
            FluidRegistryEntry<?> re = (FluidRegistryEntry<?>) entry;
            // Potions are created "on demand" rather than all upfront
            // so we hack around that by adding potions here.
            if (re.backingRegistry == Registries.POTION) {
                Potion potion = (Potion) re.backingObject;
                return get(potion);
            }
            // custom, simple, modded fluids are also created "on demand"
            if (re.backingRegistry == Registries.FLUID) {
                Fluid fluid = (Fluid) re.backingObject;
                return get(fluid);
            }
            return OTHERS.get(re);
        }
        throw new IllegalArgumentException("Unknown FluidEntry " + entry.getClass());
    }

    /** @return A copy of all the {@link FluidRegistryEntry}s registered. */
    public static synchronized Set<FluidRegistryEntry<?>> getRegistryFluidIds() {
        return new HashSet<>(OTHERS.keySet());
    }

    /** @return A copy of all the {@link FluidFloatingEntry}s registered. */
    public static synchronized Set<FluidFloatingEntry> getFloatingFluidIds() {
        return new HashSet<>(FLOATING.keySet());
    }
}
