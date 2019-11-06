/*
 * Copyright (c) 2019 AlexIIL
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package alexiil.mc.lib.attributes.fluid.volume;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.fluid.BaseFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import alexiil.mc.lib.attributes.fluid.volume.NormalFluidKey.NormalFluidKeyBuilder;

public class FluidKeys {

    public static final Identifier MISSING_SPRITE = new Identifier("minecraft", "missingno");

    public static final NormalFluidKey EMPTY;
    public static final NormalFluidKey LAVA;
    public static final BiomeSourcedFluidKey WATER;

    private static final Map<FluidRegistryEntry<?>, FluidKey> MASTER_MAP = new HashMap<>();

    private static final Map<Fluid, FluidKey> FLUIDS = new IdentityHashMap<>();
    private static final Map<Potion, FluidKey> POTIONS = new IdentityHashMap<>();

    static {
        // Empty doesn't have a proper sprite or text component because it doesn't ever make sense to use it.
        EMPTY = new NormalFluidKeyBuilder(Fluids.EMPTY, //
            MISSING_SPRITE, //
            new LiteralText("!EMPTY FLUID!")//
        ).build();
        LAVA = createImplicitVanillaFluid(Fluids.LAVA);
        WATER = WaterFluidKey.INSTANCE;

        put(Fluids.EMPTY, EMPTY);
        put(Fluids.LAVA, LAVA);
        put(Fluids.WATER, WATER);
        put(Potions.EMPTY, EMPTY);
        put(Potions.WATER, WATER);
    }

    public static void put(Fluid fluid, FluidKey fluidKey) {
        FLUIDS.put(fluid, fluidKey);
        MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
        if (fluid instanceof BaseFluid) {
            FLUIDS.put(((BaseFluid) fluid).getStill(), fluidKey);
            FLUIDS.put(((BaseFluid) fluid).getFlowing(), fluidKey);
        }
    }

    public static void put(Potion potion, FluidKey fluidKey) {
        POTIONS.put(potion, fluidKey);
        MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
    }

    /** Removes a fluid entry from this map.
     * 
     * @deprecated Because I think fluids are meant to be all statically created? */
    @Deprecated
    public static void remove(Fluid fluid) {
        FLUIDS.remove(fluid);
    }

    /** @return Null if the passed fluid is null, or a non-null {@link FluidKey}. */
    public static FluidKey get(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        FluidKey fluidKey = FLUIDS.get(fluid);
        if (fluidKey == null) {
            if (fluid instanceof BaseFluid) {
                BaseFluid base = (BaseFluid) fluid;
                fluid = base.getStill();
                if (fluid == null) {
                    throw new IllegalStateException("fluid.getStill() returned a null fluid! (from " + fluid + ")");
                }
            }
            fluidKey = createImplicitVanillaFluid(fluid);
            put(fluid, fluidKey);
        }
        return fluidKey;
    }

    private static ImplicitVanillaFluidKey createImplicitVanillaFluid(Fluid fluid) {
        Block block = fluid.getDefaultState().getBlockState().getBlock();
        Text name = new TranslatableText(block.getTranslationKey());
        return new ImplicitVanillaFluidKey(NormalFluidKey.builder(fluid, MISSING_SPRITE, name));
    }

    public static FluidKey get(Potion potion) {
        FluidKey fluidKey = POTIONS.get(potion);
        if (fluidKey == null) {
            fluidKey = new PotionFluidKey(potion);
            POTIONS.put(potion, fluidKey);
            MASTER_MAP.put(fluidKey.registryEntry, fluidKey);
        }
        return fluidKey;
    }

    @Nullable
    static FluidKey get(FluidRegistryEntry<?> entry) {
        FluidKey fluidKey = MASTER_MAP.get(entry);
        // Potions are created "on demand" rather than all upfront
        // so we hack around that by adding potions here.
        if (fluidKey == null && entry.backingRegistry == Registry.POTION) {
            Potion potion = (Potion) entry.backingObject;
            return get(potion);
        }
        // custom, simple, modded fluids are also created "on demand"
        if (fluidKey == null && entry.backingRegistry == Registry.FLUID) {
            Fluid fluid = (Fluid) entry.backingObject;
            return get(fluid);
        }
        return fluidKey;
    }
}
